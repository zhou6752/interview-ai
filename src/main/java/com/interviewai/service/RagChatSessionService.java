package com.interviewai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.common.exception.BusinessException;
import com.interviewai.common.exception.ErrorCode;
import com.interviewai.entity.RagChatMessage;
import com.interviewai.entity.RagChatSession;
import com.interviewai.repository.RagChatMessageRepository;
import com.interviewai.repository.RagChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RagChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(RagChatSessionService.class);
    private static final int MAX_HISTORY = 10;
    private static final int MAX_MSG_LENGTH = 500;
    private static final int MAX_RECENT_USER_QUESTIONS = 2;

    private final RagChatSessionRepository sessionRepo;
    private final RagChatMessageRepository messageRepo;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper;

    public RagChatSessionService(RagChatSessionRepository sessionRepo,
                                  RagChatMessageRepository messageRepo,
                                  KnowledgeBaseService knowledgeBaseService,
                                  ObjectMapper objectMapper) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.knowledgeBaseService = knowledgeBaseService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RagChatSession createSession(Long userId, String title, List<String> kbNames) {
        RagChatSession session = new RagChatSession();
        session.setUserId(userId);
        session.setTitle(title != null && !title.isBlank() ? title : "新对话");
        try {
            session.setKnowledgeBaseIds(objectMapper.writeValueAsString(kbNames != null ? kbNames : List.of()));
        } catch (JsonProcessingException e) {
            session.setKnowledgeBaseIds("[]");
        }
        RagChatSession saved = sessionRepo.save(session);
        log.info("创建 RAG 会话: id={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    public List<RagChatSession> listSessions(Long userId) {
        return sessionRepo.findByUserIdOrderByUpdateTimeDesc(userId);
    }

    public List<Map<String, Object>> getMessages(Long sessionId, Long userId) {
        RagChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
        return messageRepo.findBySessionIdOrderByMessageOrderAsc(sessionId).stream()
                .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("role", m.getType() == RagChatMessage.MessageType.USER ? "user" : "ai");
                    map.put("content", m.getContent());
                    return map;
                }).toList();
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        RagChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
        sessionRepo.delete(session);
    }

    @Transactional
    public Long prepareStreamMessage(Long sessionId, Long userId, String question) {
        RagChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }

        List<RagChatMessage> messages = messageRepo.findBySessionIdOrderByMessageOrderAsc(sessionId);
        int nextOrder = messages.isEmpty() ? 0 : messages.get(messages.size() - 1).getMessageOrder() + 1;

        RagChatMessage userMsg = new RagChatMessage();
        userMsg.setSession(session);
        userMsg.setMessageOrder(nextOrder);
        userMsg.setType(RagChatMessage.MessageType.USER);
        userMsg.setContent(question);
        messageRepo.save(userMsg);

        RagChatMessage aiMsg = new RagChatMessage();
        aiMsg.setSession(session);
        aiMsg.setMessageOrder(nextOrder + 1);
        aiMsg.setType(RagChatMessage.MessageType.AI);
        aiMsg.setContent("");
        messageRepo.save(aiMsg);

        session.setUpdateTime(LocalDateTime.now());
        if (messages.isEmpty()) {
            session.setTitle(question.length() > 30 ? question.substring(0, 30) + "..." : question);
        }
        sessionRepo.save(session);

        return aiMsg.getId();
    }

    public SseEmitter getStreamAnswer(Long sessionId, Long userId, String question) {

        RagChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }

        List<RagChatMessage> recent = messageRepo.findRecentBySessionId(sessionId, PageRequest.of(0, MAX_HISTORY + 1));

        List<String> recentUserQuestions = extractRecentUserQuestions(recent);

        String questionForSearch = question;
        if (!recentUserQuestions.isEmpty()) {
            questionForSearch = question + " " + String.join(" ", recentUserQuestions);
        }

        String historyContext = buildTrimmedHistoryContext(recent);
        List<String> kbNames = parseKbNames(session.getKnowledgeBaseIds());

        log.info("RAG 多轮检索: kbNames={}, searchQuery='{}', historyLen={}",
                kbNames, questionForSearch, historyContext.length());

        StringBuilder fullContent = new StringBuilder();
        SseEmitter emitter = knowledgeBaseService.ragChatStream(
                userId, questionForSearch, kbNames, historyContext, fullContent::append);

        emitter.onCompletion(() -> {
            if (fullContent.length() > 0) {
                completeStreamMessageBySession(sessionId, fullContent.toString());
            }
        });
        emitter.onError(e -> {
            String content = fullContent.length() > 0 ? fullContent.toString() : "【错误】" + e.getMessage();
            completeStreamMessageBySession(sessionId, content);
        });

        return emitter;
    }

    @Transactional
    public void completeStreamMessageBySession(Long sessionId, String fullContent) {
        List<RagChatMessage> messages = messageRepo.findBySessionIdOrderByMessageOrderAsc(sessionId);
        if (!messages.isEmpty()) {
            RagChatMessage last = messages.get(messages.size() - 1);
            if (last.getType() == RagChatMessage.MessageType.AI && last.getContent().isEmpty()) {
                last.setContent(fullContent);
                messageRepo.save(last);
            }
        }
    }

    private List<String> extractRecentUserQuestions(List<RagChatMessage> recent) {
        List<String> questions = new ArrayList<>();
        int count = 0;
        for (int i = recent.size() - 1; i >= 0 && count < MAX_RECENT_USER_QUESTIONS; i--) {
            RagChatMessage msg = recent.get(i);
            if (msg.getType() == RagChatMessage.MessageType.USER
                    && msg.getContent() != null && !msg.getContent().isBlank()) {
                String q = msg.getContent().trim();
                if (q.length() > 50) q = q.substring(0, 50);
                questions.add(0, q);
                count++;
            }
        }
        return questions;
    }

    private String buildTrimmedHistoryContext(List<RagChatMessage> recent) {
        if (recent.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = recent.size() - 1; i >= 0 && count < 5; i--) {
            RagChatMessage msg = recent.get(i);
            if (msg.getContent() == null || msg.getContent().isBlank()) continue;
            String content = msg.getContent().trim();
            if (content.length() > MAX_MSG_LENGTH) {
                content = content.substring(0, MAX_MSG_LENGTH) + "...";
            }
            String role = msg.getType() == RagChatMessage.MessageType.USER ? "用户" : "AI";
            sb.insert(0, role + ": " + content + "\n");
            count++;
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseKbNames(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}