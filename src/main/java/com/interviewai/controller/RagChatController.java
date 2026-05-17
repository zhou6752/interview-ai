package com.interviewai.controller;

import com.interviewai.common.result.Result;
import com.interviewai.common.exception.BusinessException;
import com.interviewai.common.exception.ErrorCode;
import com.interviewai.entity.RagChatSession;
import com.interviewai.entity.User;
import com.interviewai.repository.UserRepository;
import com.interviewai.service.RagChatSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@RestController
@RequestMapping("/api/rag-chat")
public class RagChatController {

    private static final Logger log = LoggerFactory.getLogger(RagChatController.class);

    private final RagChatSessionService sessionService;
    private final UserRepository userRepository;

    public RagChatController(RagChatSessionService sessionService, UserRepository userRepository) {
        this.sessionService = sessionService;
        this.userRepository = userRepository;
    }

    @PostMapping("/sessions")
    public Result<Map<String, Object>> createSession(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        String title = (String) body.getOrDefault("title", null);
        @SuppressWarnings("unchecked")
        List<String> kbNames = (List<String>) body.getOrDefault("knowledgeBaseNames", List.of());
        RagChatSession session = sessionService.createSession(userId, title, kbNames);
        Map<String, Object> data = new HashMap<>();
        data.put("id", session.getId());
        data.put("title", session.getTitle());
        data.put("createTime", session.getCreateTime());
        return Result.success(data);
    }

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> listSessions() {
        Long userId = getCurrentUserId();
        List<RagChatSession> sessions = sessionService.listSessions(userId);
        List<Map<String, Object>> list = sessions.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("title", s.getTitle());
            m.put("updateTime", s.getUpdateTime());
            return m;
        }).toList();
        return Result.success(list);
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<List<Map<String, Object>>> getMessages(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return Result.success(sessionService.getMessages(id, userId));
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        sessionService.deleteSession(id, userId);
        return Result.success(null);
    }

    @PostMapping("/sessions/{id}/messages/stream")
    public SseEmitter sendMessageStream(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        String question = body.getOrDefault("question", "");
        if (question.isBlank()) {
            SseEmitter err = new SseEmitter();
            err.completeWithError(new RuntimeException("问题不能为空"));
            return err;
        }

        try {
            sessionService.prepareStreamMessage(id, userId, question);
            return sessionService.getStreamAnswer(id, userId, question);
        } catch (Exception e) {
            log.warn("RAG 流式回答启动失败: {}", e.getMessage());
            SseEmitter err = new SseEmitter(5000L);
            try {
                err.send(SseEmitter.event().data("【错误】" + e.getMessage()));
            } catch (Exception ignored) {}
            err.complete();
            return err;
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            Long userId = userRepository.findByUsername(auth.getName())
                    .map(User::getId).orElse(null);
            if (userId == null) {
                throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户身份验证失败");
            }
            return userId;
        }
        throw new BusinessException(ErrorCode.LOGIN_FAILED, "请先登录");
    }
}
