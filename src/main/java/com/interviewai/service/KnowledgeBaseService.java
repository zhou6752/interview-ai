package com.interviewai.service;

import com.interviewai.config.CustomApiKeyFilter;
import com.interviewai.config.DynamicChatClientFactory;
import com.interviewai.entity.KnowledgeDocument;
import com.interviewai.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private static final int TOP_K = 5;
    private static final int MAX_CONTEXT_TOKENS = 4000;
    private static final double SEARCH_MIN_SCORE = 0.2;
    private static final int MIN_QUESTION_LENGTH_FOR_REWRITE = 15;
    private static final long REWRITE_CACHE_TTL_MS = 300_000;

    private final KnowledgeBaseVectorService vectorService;
    private final KnowledgeDocumentRepository documentRepository;
    private final DynamicChatClientFactory chatClientFactory;

    private final Map<String, CachedRewrite> rewriteCache = new ConcurrentHashMap<>();

    public KnowledgeBaseService(KnowledgeBaseVectorService vectorService,
                                 KnowledgeDocumentRepository documentRepository,
                                 DynamicChatClientFactory chatClientFactory) {
        this.vectorService = vectorService;
        this.documentRepository = documentRepository;
        this.chatClientFactory = chatClientFactory;
    }

    @Transactional
    public UploadResult uploadDocument(Long userId, String documentName, String rawText,
                                        String fileName, Long fileSize, String category) {
        deleteDocument(userId, documentName);

        int stored = vectorService.vectorize(userId, documentName, rawText);
        boolean vectorized = stored > 0;

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setUserId(userId);
        doc.setDocumentName(documentName);
        doc.setFileName(fileName);
        doc.setFileSize(fileSize);
        doc.setCategory(category != null ? category : "其他");
        doc.setChunkCount(vectorized ? stored : 0);
        doc.setQueryCount(0);
        doc.setVectorStatus(vectorized ? "COMPLETED"
                : (vectorService.isAvailable() ? "FAILED" : "PG_UNAVAILABLE"));
        doc.setContent(rawText);
        doc.setCreateTime(LocalDateTime.now());
        documentRepository.save(doc);

        String hint = vectorized
                ? null
                : "当前使用本地轻量 Embedding 模型（all-MiniLM-L6-v2），搜索效果有限。建议配置云端 API（如阿里云百炼）以获得更佳效果。";

        log.info("文档入库完成: userId={}, name={}, vectorized={}", userId, documentName, vectorized);
        return new UploadResult(documentName, stored, vectorized, hint);
    }

    public record UploadResult(String documentName, int chunkCount, boolean vectorized, String hint) {}

    public String buildRagContext(Long userId, String query) {
        List<String> chunks = search(userId, query);
        if (chunks.isEmpty()) return "";
        return String.join("\n\n", chunks);
    }

    public SseEmitter ragChatStream(Long userId, String question) {
        return doRagChatStream(userId, question, List.of(), "", null, null);
    }

    public SseEmitter ragChatStream(Long userId, String question,
                                     List<String> kbNames, String historyContext,
                                     Consumer<String> contentCollector) {
        return doRagChatStream(userId, question, kbNames, historyContext, null, contentCollector);
    }

    private SseEmitter doRagChatStream(Long userId, String question,
                                        List<String> kbNames, String historyContext,
                                        String customSystemPrompt,
                                        Consumer<String> contentCollector) {
        SseEmitter emitter = new SseEmitter(180_000L);

        final String apiKey = CustomApiKeyFilter.getApiKey();
        final String model = CustomApiKeyFilter.getModel();
        final String baseUrl = CustomApiKeyFilter.getBaseUrl();

        final Boolean supportsEmb = CustomApiKeyFilter.supportsEmbedding();
        final String embApiKey = CustomApiKeyFilter.getEmbApiKey();
        final String embBaseUrl = CustomApiKeyFilter.getEmbBaseUrl();
        final String embModelName = CustomApiKeyFilter.getEmbeddingModel();

        new Thread(() -> {
            try {
                vectorService.ensureInitialized(supportsEmb, embApiKey, embBaseUrl, embModelName);

                if (!vectorService.isAvailable()) {
                    String diag = vectorService.getDiagnosticInfo();
                    log.warn("向量服务不可用: {}", diag);

                    String fallbackContext = textSearchFallback(userId, question, kbNames);
                    if (fallbackContext.isEmpty()) {
                        emitter.send(SseEmitter.event().data("向量搜索服务不可用：" + diag + "。请前往「设置」页面配置支持 Embedding 的 AI 服务（如阿里云百炼），或检查 PostgreSQL 连接。"));
                        emitter.complete();
                        return;
                    }
                    String prompt = buildSystemPrompt(fallbackContext, historyContext);
                    streamResponse(emitter, apiKey, model, baseUrl, prompt, question, contentCollector);
                    return;
                }

                String rewritten = rewriteQuery(question, apiKey, model, baseUrl);
                log.info("RAG 查询: origin='{}', rewritten='{}'", question, rewritten);

                Set<String> allChunks = new LinkedHashSet<>();

                if (kbNames != null && !kbNames.isEmpty()) {
                    for (String kbName : kbNames) {
                        if (kbName != null && !kbName.isBlank()) {
                            List<String> docChunks = searchByDocumentName(userId, rewritten, kbName);
                            if (!docChunks.isEmpty()) {
                                try {
                                    documentRepository.incrementQueryCount(userId, kbName);
                                } catch (Exception e) {
                                    log.warn("更新知识库提问计数失败(不影响回答): userId={}, kbName={}, err={}",
                                            userId, kbName, e.getMessage());
                                }
                            }
                            allChunks.addAll(docChunks);
                        }
                    }
                }

                if (allChunks.isEmpty()) {
                    List<String> globalChunks = search(userId, rewritten);
                    allChunks.addAll(globalChunks);
                }

                List<String> orderedChunks = new ArrayList<>(allChunks);
                orderedChunks = trimByTokenBudget(orderedChunks, historyContext);

                String context = orderedChunks.isEmpty() ? "" : String.join("\n\n---\n\n", orderedChunks);

                if (customSystemPrompt != null && !customSystemPrompt.isBlank()) {
                    String prompt = customSystemPrompt;
                    if (!context.isEmpty()) {
                        prompt += "\n\n资料：\n" + context;
                    }
                    streamResponse(emitter, apiKey, model, baseUrl, prompt, question, contentCollector);
                } else {
                    if (context.isEmpty()) {
                        emitter.send(SseEmitter.event().data("未在知识库中找到相关内容，请换个问法试试。"));
                        emitter.complete();
                        return;
                    }
                    String prompt = buildSystemPrompt(context, historyContext);
                    streamResponse(emitter, apiKey, model, baseUrl, prompt, question, contentCollector);
                }
            } catch (Exception e) {
                log.error("RAG 流式回答处理异常: {}", e.getMessage(), e);
                try { emitter.send(SseEmitter.event().data("【错误】回答生成过程中发生异常，请稍后重试。")); } catch (Exception ignored) {}
                emitter.complete();
            }
        }).start();

        return emitter;
    }

    private void streamResponse(SseEmitter emitter, String apiKey, String model, String baseUrl,
                                 String systemPrompt, String question, Consumer<String> contentCollector) {
        ChatClient chatClient = chatClientFactory.create(apiKey, model, baseUrl);
        chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .stream()
                .content()
                .doOnNext(text -> {
                    if (text != null && !text.isEmpty()) {
                        try { emitter.send(SseEmitter.event().data(text)); } catch (Exception ignored) {}
                        if (contentCollector != null) contentCollector.accept(text);
                    }
                })
                .doOnComplete(() -> emitter.complete())
                .doOnError(e -> {
                    try { emitter.send(SseEmitter.event().data("【错误】回答生成失败：" + e.getMessage())); } catch (Exception ignored) {}
                    emitter.complete();
                })
                .subscribe();
    }

    private String buildSystemPrompt(String context, String historyContext) {
        StringBuilder sb = new StringBuilder();
        if (historyContext != null && !historyContext.isBlank()) {
            sb.append("对话历史：\n").append(historyContext).append("\n\n");
        }
        sb.append("根据以下资料回答用户问题。如果资料不足以回答，请如实说明。\n\n资料：\n").append(context);
        return sb.toString();
    }

    private List<String> search(Long userId, String query) {
        try {
            List<Document> docs = vectorService.search(userId, query, TOP_K, SEARCH_MIN_SCORE);
            if (!docs.isEmpty()) {
                return docs.stream().map(Document::getContent).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("向量搜索失败: {}", e.getMessage());
        }
        return List.of();
    }

    private List<String> searchByDocumentName(Long userId, String query, String documentName) {
        try {
            List<Document> docs = vectorService.searchByDocument(userId, query, documentName, TOP_K, SEARCH_MIN_SCORE);
            if (!docs.isEmpty()) {
                return docs.stream().map(Document::getContent).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("按文档 {} 向量搜索失败: {}", documentName, e.getMessage());
        }
        return List.of();
    }

    private List<String> trimByTokenBudget(List<String> chunks, String historyContext) {
        int historyTokens = estimateTokens(historyContext);
        int availableForContext = MAX_CONTEXT_TOKENS - historyTokens - 500;
        if (availableForContext <= 0) availableForContext = MAX_CONTEXT_TOKENS / 2;

        List<String> trimmed = new ArrayList<>();
        int usedTokens = 0;
        for (String chunk : chunks) {
            int chunkTokens = estimateTokens(chunk);
            if (usedTokens + chunkTokens > availableForContext) {
                if (trimmed.isEmpty()) {
                    trimmed.add(chunk.length() > 500 ? chunk.substring(0, 500) : chunk);
                }
                break;
            }
            trimmed.add(chunk);
            usedTokens += chunkTokens;
        }
        return trimmed;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int chineseChars = 0;
        int otherTokens = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION) {
                chineseChars++;
            }
        }
        String nonChinese = text.replaceAll("[\\u4e00-\\u9fff\\u3000-\\u303f\\uff00-\\uffef]", " ");
        String[] words = nonChinese.split("\\s+");
        for (String w : words) { if (!w.isBlank()) otherTokens++; }
        return (int) (chineseChars * 1.5 + otherTokens * 1.3);
    }

    private String textSearchFallback(Long userId, String query, List<String> kbNames) {
        List<String> keywords = extractKeywords(query);
        List<String> results = new ArrayList<>();
        List<KnowledgeDocument> docs;

        if (kbNames != null && !kbNames.isEmpty()) {
            docs = new ArrayList<>();
            for (String name : kbNames) {
                docs.addAll(documentRepository.findByUserIdAndDocumentName(userId, name));
            }
        } else {
            docs = documentRepository.findByUserIdOrderByCreateTimeDesc(userId);
        }

        for (KnowledgeDocument doc : docs) {
            if (doc.getContent() == null) continue;
            String content = doc.getContent();
            int score = 0;
            String lowerContent = content.toLowerCase();
            for (String kw : keywords) {
                int idx = 0;
                while ((idx = lowerContent.indexOf(kw.toLowerCase(), idx)) != -1) {
                    score++;
                    idx += kw.length();
                }
            }
            if (score > 0) {
                String header = "【" + doc.getDocumentName() + "】\n";
                int start = Math.max(0, content.indexOf(keywords.get(0)));
                int end = Math.min(content.length(), start + 1000);
                String snippet = content.substring(start, end);
                results.add(header + snippet);
            }
            if (results.size() >= TOP_K) break;
        }

        if (results.isEmpty()) {
            for (KnowledgeDocument doc : docs) {
                if (doc.getContent() != null) {
                    String header = "【" + doc.getDocumentName() + "】\n";
                    String snippet = doc.getContent().length() > 500
                            ? doc.getContent().substring(0, 500) : doc.getContent();
                    results.add(header + snippet);
                    break;
                }
            }
        }

        return String.join("\n\n---\n\n", results);
    }

    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();
        String[] parts = query.split("[\\s,，。.!！?？]+");
        for (String p : parts) {
            if (p.length() >= 2) {
                keywords.add(p);
                if (p.length() >= 4) {
                    for (int i = 0; i <= p.length() - 2; i++) {
                        keywords.add(p.substring(i, i + 2));
                    }
                }
            }
        }
        if (keywords.isEmpty()) keywords.add(query);
        return keywords;
    }

    private String rewriteQuery(String question, String apiKey, String model, String baseUrl) {
        if (question == null || question.length() < MIN_QUESTION_LENGTH_FOR_REWRITE) return question;
        if (!containsChinese(question) && question.length() < 20) return question;
        if (apiKey == null || apiKey.isBlank()) return question;

        CachedRewrite cached = rewriteCache.get(question);
        if (cached != null && System.currentTimeMillis() - cached.timestamp < REWRITE_CACHE_TTL_MS) {
            return cached.result;
        }

        try {
            ChatClient chatClient = chatClientFactory.create(apiKey, model, baseUrl);
            String rewritten = chatClient.prompt()
                    .user("把以下问题改写成用于知识库检索的关键词短语，去掉口语化表达，保留核心概念。只输出改写后的短语，不要解释。\n\n问题：" + question)
                    .call()
                    .content();
            String result = (rewritten != null && !rewritten.isBlank()
                    && !rewritten.trim().equals(question.trim())) ? rewritten.trim() : question;
            rewriteCache.put(question, new CachedRewrite(result, System.currentTimeMillis()));
            return result;
        } catch (Exception e) {
            log.warn("Query rewrite 失败: {}", e.getMessage());
            return question;
        }
    }

    private boolean containsChinese(String text) {
        if (text == null) return false;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) return true;
        }
        return false;
    }

    private static class CachedRewrite {
        final String result;
        final long timestamp;
        CachedRewrite(String result, long timestamp) { this.result = result; this.timestamp = timestamp; }
    }

    public List<Map<String, Object>> getDocumentList(Long userId) {
        return documentRepository.findByUserIdOrderByCreateTimeDesc(userId).stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", d.getId());
                    m.put("name", d.getDocumentName());
                    m.put("fileName", d.getFileName());
                    m.put("fileSize", d.getFileSize());
                    m.put("category", d.getCategory());
                    m.put("chunkCount", d.getChunkCount());
                    m.put("queryCount", d.getQueryCount());
                    m.put("vectorStatus", d.getVectorStatus());
                    m.put("createTime", d.getCreateTime());
                    return m;
                }).collect(Collectors.toList());
    }

    public Map<String, Object> getStats(Long userId) {
        List<KnowledgeDocument> docs = documentRepository.findByUserIdOrderByCreateTimeDesc(userId);
        long totalDocs = docs.size();
        long totalQueries = docs.stream().mapToLong(d -> d.getQueryCount() != null ? d.getQueryCount() : 0).sum();
        Map<String, Object> stats = new HashMap<>();
        stats.put("documentCount", totalDocs);
        stats.put("totalQueries", totalQueries);
        stats.put("totalChunks", vectorService.getPgChunkCount(userId));
        return stats;
    }

    @Transactional
    public void deleteDocument(Long userId, String documentName) {
        documentRepository.deleteByUserIdAndDocumentName(userId, documentName);
        vectorService.deleteDocument(userId, documentName);
        log.info("删除知识文档: userId={}, name={}", userId, documentName);
    }
}