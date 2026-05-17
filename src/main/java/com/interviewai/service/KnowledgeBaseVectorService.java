package com.interviewai.service;

import com.interviewai.config.CustomApiKeyFilter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseVectorService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseVectorService.class);

    @Value("${app.pgvector.url:}")
    private String pgUrl;

    @Value("${app.pgvector.username:}")
    private String pgUsername;

    @Value("${app.pgvector.password:}")
    private String pgPassword;

    @Value("${app.pgvector.driver-class-name:org.postgresql.Driver}")
    private String pgDriver;

    private PgVectorStore vectorStore;
    private EmbeddingModel embeddingModel;
    private TokenTextSplitter textSplitter;
    private boolean available = false;
    private boolean pgConnectionFailed = false;
    private String pgFailReason = null;
    private List<Document> lastChunks;
    private DataSource sharedDataSource;
    private JdbcTemplate jdbcTemplate;

    private Boolean overrideSupportsEmb;
    private String overrideEmbApiKey;
    private String overrideEmbBaseUrl;
    private String overrideEmbModelName;

    private final SimpleLocalEmbeddingService simpleLocalEmbedding;

    public KnowledgeBaseVectorService(SimpleLocalEmbeddingService simpleLocalEmbedding) {
        this.simpleLocalEmbedding = simpleLocalEmbedding;
    }

    public synchronized void ensureInitialized(Boolean supportsEmb, String embApiKey,
                                                String embBaseUrl, String embModelName) {
        this.overrideSupportsEmb = supportsEmb;
        this.overrideEmbApiKey = embApiKey;
        this.overrideEmbBaseUrl = embBaseUrl;
        this.overrideEmbModelName = embModelName;
        ensureInitialized();
        this.overrideSupportsEmb = null;
        this.overrideEmbApiKey = null;
        this.overrideEmbBaseUrl = null;
        this.overrideEmbModelName = null;
    }

    @PostConstruct
    void init() {
        if (pgUrl == null || pgUrl.isBlank()) {
            log.warn("pgvector 未配置，向量搜索不可用。请在 application.yml 中配置 app.pgvector.* 参数");
            pgConnectionFailed = true;
            pgFailReason = "pgvector 未配置 (app.pgvector.url 为空)";
            return;
        }
        try {
            Class.forName(pgDriver);
            this.textSplitter = new TokenTextSplitter();
            this.sharedDataSource = new DriverManagerDataSource(pgUrl, pgUsername, pgPassword);
            this.jdbcTemplate = new JdbcTemplate(sharedDataSource);

            java.sql.Connection testConn = sharedDataSource.getConnection();
            testConn.close();
            log.info("KnowledgeBaseVectorService JDBC 连接测试成功: {}", pgUrl);
        } catch (Exception e) {
            pgConnectionFailed = true;
            pgFailReason = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("pgvector JDBC 连接失败! 请检查:\n" +
                    "  1. PostgreSQL 服务是否运行 (端口 {})\n" +
                    "  2. 用户名/密码是否正确 (当前: {} / {})\n" +
                    "  3. 数据库是否存在\n" +
                    "  错误详情: {}", pgUrl, pgUsername, pgPassword, pgFailReason);
        }
    }

    private synchronized void ensureInitialized() {
        if (available) return;
        if (pgConnectionFailed || pgUrl == null || pgUrl.isBlank()) return;
        try {
            JdbcTemplate jdbc = jdbcTemplate;

            EmbeddingModel emb = getOrCreateEmbeddingModel();
            if (emb == null) {
                log.warn("EmbeddingModel 不可用，向量存储未启用");
                return;
            }
            this.embeddingModel = emb;
            this.vectorStore = new PgVectorStore(jdbc, emb);
            this.vectorStore.afterPropertiesSet();
            this.available = true;
            log.info("PgVectorStore 延迟初始化完成");
        } catch (Exception e) {
            log.warn("PgVectorStore 初始化失败: {}", e.getMessage());
        }
    }

    private EmbeddingModel getOrCreateEmbeddingModel() {
        if (this.embeddingModel != null) return this.embeddingModel;

        Boolean supportsEmb = overrideSupportsEmb != null
                ? overrideSupportsEmb : CustomApiKeyFilter.supportsEmbedding();

        if (Boolean.TRUE.equals(supportsEmb)) {
            try {
                String apiKey = overrideEmbApiKey != null
                        ? overrideEmbApiKey : CustomApiKeyFilter.getEmbApiKey();
                String baseUrl = overrideEmbBaseUrl != null
                        ? overrideEmbBaseUrl : CustomApiKeyFilter.getEmbBaseUrl();
                if (apiKey == null || apiKey.isBlank()) apiKey = CustomApiKeyFilter.getApiKey();
                if (baseUrl == null || baseUrl.isBlank()) baseUrl = CustomApiKeyFilter.getBaseUrl();
                String embModel = overrideEmbModelName != null
                        ? overrideEmbModelName : CustomApiKeyFilter.getEmbeddingModel();
                if (embModel == null || embModel.isBlank()) embModel = "text-embedding-v3";

                if (apiKey == null || apiKey.isBlank()) {
                    log.info("远端 API Key 未配置，将尝试本地 ONNX 模型");
                } else {
                    String effectiveUrl = (baseUrl != null && !baseUrl.isBlank())
                            ? baseUrl : "https://dashscope.aliyuncs.com/compatible-mode";
                    OpenAiApi api = new OpenAiApi(effectiveUrl, apiKey);
                    OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                            .withModel(embModel).build();
                    this.embeddingModel = new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
                    log.info("Embedding 就绪: 远端 API (model={})", embModel);
                    return embeddingModel;
                }
            } catch (Exception e) {
                log.warn("远端 Embedding 初始化失败: {}，回退本地模型", e.getMessage());
            }
        }

        this.embeddingModel = simpleLocalEmbedding;
        log.info("Embedding 就绪: 本地 n-gram 随机投影 (建议配置云端 API 以获得更佳效果)");
        return embeddingModel;
    }

    public void resetEmbeddingModel() {
        this.embeddingModel = null;
    }

    public boolean isAvailable() { return available && vectorStore != null; }

    public String getDiagnosticInfo() {
        if (isAvailable()) return "向量服务就绪";
        if (pgConnectionFailed && pgFailReason != null) return "PostgreSQL 连接失败: " + pgFailReason;
        if (pgUrl == null || pgUrl.isBlank()) return "pgvector 未配置";
        if (embeddingModel == null) return "无 Embedding 模型，请配置支持向量化的 AI 服务（如阿里云百炼）";
        return "向量存储异常";
    }

    public int vectorize(Long userId, String documentName, String content) {
        ensureInitialized();
        if (!isAvailable()) return 0;
        if (embeddingModel == null) {
            log.warn("EmbeddingModel 不可用，跳过向量化");
            return 0;
        }

        deleteDocument(userId, documentName);

        List<Document> chunks = textSplitter.apply(List.of(new Document(content)));
        log.info("文本分块完成: {} 个 chunks", chunks.size());

        int stored = 0;
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            String text = chunk.getContent();
            if (text == null || text.isBlank()) continue;
            try {
                float[] embedding = embeddingModel.embed(new Document(text));
                if (embedding == null || embedding.length == 0) continue;
                Document docWithEmbedding = new Document(text);
                docWithEmbedding.getMetadata().put("user_id", userId);
                docWithEmbedding.getMetadata().put("document_name", documentName);
                docWithEmbedding.getMetadata().put("chunk_index", i);
                vectorStore.add(List.of(docWithEmbedding));
                stored++;
            } catch (Exception e) {
                log.warn("向量化 chunk {} 失败: {}", i, e.getMessage());
            }
        }
        log.info("向量化完成: userId={}, name={}, stored={}/{}", userId, documentName, stored, chunks.size());
        this.lastChunks = chunks;
        return stored;
    }

    public List<Document> getLastChunks() {
        return lastChunks;
    }

    public void deleteDocument(long userId, String documentName) {
        if (!isAvailable()) return;
        try {
            jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'user_id' = ? AND metadata->>'document_name' = ?",
                String.valueOf(userId), documentName
            );
        } catch (Exception e) {
            log.warn("删除向量失败: {}", e.getMessage());
        }
    }

    public List<Document> search(long userId, String query, int topK, double minScore) {
        ensureInitialized();
        if (!isAvailable() || embeddingModel == null) return List.of();
        try {
            SearchRequest request = SearchRequest.query(query)
                    .withTopK(topK * 3)
                    .withSimilarityThreshold(minScore);
            List<Document> raw = vectorStore.similaritySearch(request);
            return raw.stream()
                    .filter(d -> String.valueOf(userId).equals(String.valueOf(d.getMetadata().get("user_id"))))
                    .limit(topK)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("向量搜索失败: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Document> searchByDocument(long userId, String query, String documentName, int topK, double minScore) {
        ensureInitialized();
        if (!isAvailable() || embeddingModel == null) return List.of();
        try {
            SearchRequest request = SearchRequest.query(query)
                    .withTopK(topK * 3)
                    .withSimilarityThreshold(minScore);
            List<Document> raw = vectorStore.similaritySearch(request);
            return raw.stream()
                    .filter(d -> String.valueOf(userId).equals(String.valueOf(d.getMetadata().get("user_id")))
                            && documentName.equals(d.getMetadata().get("document_name")))
                    .limit(topK)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("按文档 {} 向量搜索失败: {}", documentName, e.getMessage());
            return List.of();
        }
    }

    public List<String> getPgDocumentList(long userId) {
        if (!isAvailable()) return List.of();
        try {
            return jdbcTemplate.queryForList(
                "SELECT DISTINCT metadata->>'document_name' FROM vector_store WHERE metadata->>'user_id' = ?",
                String.class, String.valueOf(userId)
            );
        } catch (Exception e) {
            log.warn("pgvector 文档列表查询失败: {}", e.getMessage());
            return List.of();
        }
    }

    public long getPgChunkCount(long userId) {
        if (!isAvailable()) return 0;
        try {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vector_store WHERE metadata->>'user_id' = ?",
                Long.class, String.valueOf(userId)
            );
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}