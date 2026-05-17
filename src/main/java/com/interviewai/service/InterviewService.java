package com.interviewai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.config.CustomApiKeyFilter;
import com.interviewai.config.DynamicChatClientFactory;
import com.interviewai.dto.InterviewQuestionItem;
import com.interviewai.dto.InterviewReport;
import com.interviewai.dto.InterviewStartRequest;
import com.interviewai.dto.QAPair;
import com.interviewai.dto.SessionContext;
import com.interviewai.entity.InterviewRecord;
import com.interviewai.entity.User;
import com.interviewai.repository.InterviewRecordRepository;
import com.interviewai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final DynamicChatClientFactory chatClientFactory;
    private final QuestionGenerationService questionGenerationService;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final InterviewRecordRepository recordRepository;
    private final EvaluationService evaluationService;

    public InterviewService(DynamicChatClientFactory chatClientFactory,
                            QuestionGenerationService questionGenerationService,
                            SessionService sessionService,
                            ObjectMapper objectMapper,
                            UserRepository userRepository,
                            InterviewRecordRepository recordRepository,
                            EvaluationService evaluationService) {
        this.chatClientFactory = chatClientFactory;
        this.questionGenerationService = questionGenerationService;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.evaluationService = evaluationService;
    }

    public InterviewStartResult startInterview(InterviewStartRequest request) {
        Long userId = getCurrentUserId();
        SessionContext session;

        if (request.getSessionToken() != null && !request.getSessionToken().isEmpty()) {
            session = sessionService.getSession(request.getSessionToken());
            if (session == null) {
                throw new RuntimeException("会话不存在或已过期");
            }
            if (request.getResumeAnalysisResult() != null) {
                session.setResumeAnalysisResult(request.getResumeAnalysisResult());
            }
            if (request.getMode() != null) {
                session.setMode(request.getMode());
            }
        } else {
            if (request.getResumeAnalysisResult() != null) {
                session = sessionService.startSession(request.getPosition(), request.getResumeAnalysisResult());
            } else {
                session = sessionService.startSession(request.getPosition());
            }
            if (request.getMode() != null) {
                session.setMode(request.getMode());
            }
        }

        session.setUserId(userId);
        sessionService.updateSession(session.getToken(), session);

        // 恢复已有会话：如果 session 里已经有题目了，直接返回当前题目，不重新生成
        if (session.getQuestions() != null && !session.getQuestions().isEmpty()) {
            int idx = session.getCurrentQuestionIndex();
            if (idx >= session.getQuestions().size()) {
                idx = session.getQuestions().size() - 1;
                session.setCurrentQuestionIndex(idx);
            }
            String currentQuestion = session.getCurrentQuestionText();
            if (currentQuestion == null || currentQuestion.isBlank()) {
                currentQuestion = session.getQuestions().get(idx).getQuestion();
                session.setCurrentQuestionText(currentQuestion);
            }
            sessionService.updateSession(session.getToken(), session);
            log.info("恢复面试: session={}, userId={}, currentIndex={}, totalQuestions={}",
                    session.getToken(), userId, idx, session.getQuestions().size());
            return new InterviewStartResult(session.getToken(), currentQuestion);
        }

        String capturedApiKey = CustomApiKeyFilter.getApiKey();
        String capturedModel = CustomApiKeyFilter.getModel();
        String capturedBaseUrl = CustomApiKeyFilter.getBaseUrl();

        if (capturedApiKey == null || capturedApiKey.isBlank()) {
            throw new RuntimeException("未配置 API 密钥，请先在设置页面配置大模型服务");
        }

        List<InterviewQuestionItem> allQuestions = questionGenerationService.generateAllQuestions(session);
        session.setQuestions(allQuestions);
        session.setCurrentQuestionIndex(0);

        String firstQuestion = allQuestions.get(0).getQuestion();
        session.setCurrentQuestionText(firstQuestion);
        sessionService.updateSession(session.getToken(), session);

        saveQuestionToDb(session, allQuestions.get(0), 0);

        log.info("面试开始: session={}, userId={}, totalQuestions={}, firstQuestion={}",
                session.getToken(), userId, allQuestions.size(),
                firstQuestion.length() > 50 ? firstQuestion.substring(0, 50) + "..." : firstQuestion);

        return new InterviewStartResult(session.getToken(), firstQuestion);
    }

    public SseEmitter conductInterview(SessionContext session, String userAnswer) {
        SseEmitter emitter = new SseEmitter(180_000L);

        emitter.onTimeout(() -> log.warn("SSE 连接超时 session={}", session.getToken()));
        emitter.onError(e -> log.error("SSE 连接异常 session={}: {}", session.getToken(), e.getMessage()));

        // 捕获当前 SecurityContext，传给异步线程，避免 SSE async dispatch 时被 Spring Security 拦截
        org.springframework.security.core.context.SecurityContext ctx =
                org.springframework.security.core.context.SecurityContextHolder.getContext();

        new Thread(() -> {
            org.springframework.security.core.context.SecurityContextHolder.setContext(ctx);
            try {
                List<InterviewQuestionItem> questions = session.getQuestions();
                if (questions == null || questions.isEmpty()) {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"type\":\"error\",\"data\":\"题目列表为空，请重新开始面试\"}"));
                    emitter.complete();
                    return;
                }

                int currentIdx = session.getCurrentQuestionIndex();
                if (currentIdx >= questions.size()) {
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(Map.of(
                                    "type", "all_answered",
                                    "message", "所有问题已回答完毕"))));
                    emitter.complete();
                    return;
                }

                InterviewQuestionItem currentQ = questions.get(currentIdx);
                currentQ.setUserAnswer(userAnswer);

                QAPair pair = new QAPair(currentQ.getQuestion(), userAnswer, "");
                session.addToHistory(pair);

                updateQuestionAnswerInDb(session, currentQ, currentIdx, userAnswer);

                int nextIdx = currentIdx + 1;
                session.setCurrentQuestionIndex(nextIdx);
                sessionService.updateSession(session.getToken(), session);

                if (nextIdx < questions.size()) {
                    InterviewQuestionItem nextQ = questions.get(nextIdx);
                    saveQuestionToDb(session, nextQ, nextIdx);

                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(Map.of(
                                    "type", "next_question",
                                    "questionIndex", nextIdx,
                                    "totalQuestions", questions.size(),
                                    "question", nextQ.getQuestion(),
                                    "difficulty", nextQ.getDifficulty(),
                                    "category", nextQ.getCategory(),
                                    "isFollowUp", nextQ.getParentIndex() != null))));
                } else {
                    emitter.send(SseEmitter.event()
                            .data(objectMapper.writeValueAsString(Map.of(
                                    "type", "all_answered",
                                    "message", "面试结束，请点击生成评估报告"))));
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("答题流程异常 session={}: {}", session.getToken(), e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"type\":\"error\",\"data\":\"" + e.getMessage() + "\"}"));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            } finally {
                sessionService.releaseAnswerLock(session.getToken());
                SecurityContextHolder.clearContext();
            }
        }).start();

        return emitter;
    }

    public InterviewReport finishInterview(SessionContext session) {
        List<QAPair> history = session.getHistory();
        if (history == null || history.isEmpty()) {
            throw new RuntimeException("没有问答记录，无法生成报告");
        }

        InterviewReport report = evaluationService.evaluate(history, session.getMode());

        session.setStatus("已结束");
        session.setScore((int) report.getOverallScore());
        sessionService.updateSession(session.getToken(), session);

        saveReportToDb(session, report);

        return report;
    }

    private void saveQuestionToDb(SessionContext session, InterviewQuestionItem question, int index) {
        if (session.getUserId() == null) return;
        try {
            InterviewRecord record = new InterviewRecord();
            record.setUserId(session.getUserId());
            record.setSessionId(session.getToken());
            record.setSessionToken(session.getToken());
            record.setPosition(session.getPosition());
            record.setQuestionIndex(index);
            record.setQuestion(question.getQuestion());
            record.setCategory(question.getCategory());
            record.setDifficulty(question.getDifficulty());
            record.setStartTime(LocalDateTime.now());
            record.setStatus("IN_PROGRESS");
            recordRepository.save(record);
        } catch (Exception e) {
            log.warn("保存题目到数据库失败: {}", e.getMessage());
        }
    }

    private void updateQuestionAnswerInDb(SessionContext session, InterviewQuestionItem question,
                                           int index, String userAnswer) {
        if (session.getUserId() == null) return;
        try {
            List<InterviewRecord> records = recordRepository.findBySessionTokenOrderByQuestionIndexAsc(session.getToken());
            for (InterviewRecord record : records) {
                if (record.getQuestionIndex() != null && record.getQuestionIndex() == index) {
                    record.setUserAnswer(userAnswer);
                    recordRepository.save(record);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("更新答题到数据库失败: {}", e.getMessage());
        }
    }

    private void saveReportToDb(SessionContext session, InterviewReport report) {
        if (session.getUserId() == null) return;
        try {
            String reportJson = objectMapper.writeValueAsString(report);
            List<InterviewRecord> records = recordRepository.findBySessionTokenOrderByQuestionIndexAsc(session.getToken());
            for (InterviewRecord record : records) {
                record.setTotalScore((int) report.getOverallScore());
                record.setReportJson(reportJson);
                record.setEndTime(LocalDateTime.now());
                record.setStatus("COMPLETED");
            }
            recordRepository.saveAll(records);
        } catch (Exception e) {
            log.warn("保存报告到数据库失败: {}", e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return userRepository.findByUsername(auth.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }

    public static class InterviewStartResult {
        private final String sessionToken;
        private final String firstQuestion;

        public InterviewStartResult(String sessionToken, String firstQuestion) {
            this.sessionToken = sessionToken;
            this.firstQuestion = firstQuestion;
        }

        public String getSessionToken() {
            return sessionToken;
        }

        public String getFirstQuestion() {
            return firstQuestion;
        }
    }
}