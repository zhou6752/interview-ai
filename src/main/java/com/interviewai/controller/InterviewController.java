package com.interviewai.controller;

import com.interviewai.annotation.RateLimit;
import com.interviewai.dto.FinishInterviewRequest;
import com.interviewai.dto.InterviewAnswerRequest;
import com.interviewai.dto.InterviewReport;
import com.interviewai.dto.InterviewStartRequest;
import com.interviewai.dto.SessionContext;
import com.interviewai.entity.InterviewRecord;
import com.interviewai.repository.InterviewRecordRepository;
import com.interviewai.repository.UserRepository;
import com.interviewai.service.InterviewService;
import com.interviewai.service.SessionService;
import com.interviewai.service.SkillService;
import com.interviewai.util.PdfExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// InterviewController —— 面试流程的 API 入口
// 职责：接收 HTTP 请求、调 Service、返回结果。所有业务逻辑都在 InterviewService 里
@RestController
@RequestMapping("/api/interview")
@Tag(name = "面试", description = "核心面试流程：开始面试 + 提交回答（SSE 流式返回）")
public class InterviewController {

    private final InterviewService interviewService;
    private final SessionService sessionService;
    private final SkillService skillService;
    private final InterviewRecordRepository recordRepository;
    private final UserRepository userRepository;

    public InterviewController(InterviewService interviewService,
                               SessionService sessionService,
                               SkillService skillService,
                               InterviewRecordRepository recordRepository,
                               UserRepository userRepository) {
        this.interviewService = interviewService;
        this.sessionService = sessionService;
        this.skillService = skillService;
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    @Operation(summary = "开始面试", description = "创建新的面试会话，返回 sessionToken、总题数和第一道题")
    @RateLimit(count = 5, seconds = 60, message = "面试请求过于频繁，每分钟最多 5 次")
    @PostMapping("/start")
    public Map<String, Object> startInterview(@RequestBody InterviewStartRequest request) {
        InterviewService.InterviewStartResult result = interviewService.startInterview(request);
        SessionContext session = sessionService.getSession(result.getSessionToken());
        Map<String, Object> data = new HashMap<>();
        data.put("sessionToken", result.getSessionToken());
        data.put("currentQuestionIndex", session.getCurrentQuestionIndex());
        data.put("totalQuestions", session.getQuestions() != null ? session.getQuestions().size() : 0);
        data.put("firstQuestion", result.getFirstQuestion());
        return data;
    }

    @Operation(summary = "提交回答", description = "提交用户回答，流式返回 AI 评价和下一道面试题（SSE）。同一 session 同时只允许一个请求在处理")
    @RateLimit(count = 20, seconds = 60, message = "答题过于频繁，请控制节奏")
    @PostMapping("/answer")
    public SseEmitter answerQuestion(@RequestBody InterviewAnswerRequest request) {
        // Redis 分布式锁：防止同一 session 的并发答题请求导致状态不一致
        if (!sessionService.acquireAnswerLock(request.getSessionToken())) {
            throw new RuntimeException("当前 AI 正在回复上一道题，请稍候再提交");
        }
        SessionContext session = sessionService.getSession(request.getSessionToken());
        if (session == null) {
            sessionService.releaseAnswerLock(request.getSessionToken());
            throw new RuntimeException("面试会话不存在或已过期");
        }
        return interviewService.conductInterview(session, request.getUserAnswer());
    }

    // POST /api/interview/finish —— 结束面试
    // 从 Redis 读会话 → 交给 InterviewService 生成评估报告 → 返回 JSON
    @Operation(summary = "结束面试", description = "汇总所有问答，生成综合评估报告（包含评分、逐题点评、学习建议）")
    @PostMapping("/finish")
    public InterviewReport finishInterview(@RequestBody FinishInterviewRequest request) {
        SessionContext session = sessionService.getSession(request.getSessionToken());
        if (session == null) {
            throw new RuntimeException("面试会话不存在或已过期");
        }
        return interviewService.finishInterview(session);
    }

    @Operation(summary = "获取面试方向列表", description = "返回所有支持的技术面试方向（Java/前端/Python/通用），供前端动态渲染")
    @GetMapping("/skills")
    public List<Map<String, Object>> getSkills() {
        return skillService.getAllSkills().stream().map(skill -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", skill.id());
            m.put("name", skill.name());
            m.put("description", skill.description());
            return m;
        }).toList();
    }

    @Operation(summary = "导出面试报告 PDF", description = "根据 sessionToken 生成并下载面试报告的 PDF 文件")
    @GetMapping("/report/{sessionToken}/pdf")
    public ResponseEntity<byte[]> exportReportPdf(@PathVariable String sessionToken) {
        SessionContext session = sessionService.getSession(sessionToken);
        if (session == null || session.getHistory() == null || session.getHistory().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InterviewReport report = interviewService.finishInterview(session);
        byte[] pdfBytes = PdfExporter.exportInterviewReport(report, session.getPosition());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=interview-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @Operation(summary = "我的面试历史", description = "返回当前用户的所有历史面试记录，按 session 分组")
    @GetMapping("/history")
    public List<Map<String, Object>> getInterviewHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return List.of();
        }
        return userRepository.findByUsername(auth.getName())
                .map(user -> {
                    List<String> tokens = recordRepository.findDistinctSessionTokensByUserId(user.getId());
                    return tokens.stream().map(token -> {
                        List<InterviewRecord> records = recordRepository
                                .findByUserIdAndSessionTokenOrderByQuestionIndexAsc(user.getId(), token);
                        if (records.isEmpty()) return null;
                        InterviewRecord first = records.get(0);
                        Map<String, Object> m = new HashMap<>();
                        m.put("sessionToken", token);
                        m.put("position", first.getPosition());
                        m.put("questionCount", records.size());
                        m.put("totalScore", first.getTotalScore());
                        m.put("status", first.getStatus());
                        m.put("startTime", first.getStartTime());
                        m.put("endTime", first.getEndTime());
                        m.put("reportJson", first.getReportJson());
                        // 检查 Redis 会话是否存活
                        long ttlSeconds = sessionService.getSessionTTLSeconds(token);
                        m.put("remainingSeconds", Math.max(0, ttlSeconds));
                        if (ttlSeconds <= 0 && "IN_PROGRESS".equals(first.getStatus())) {
                            m.put("status", "EXPIRED");
                        }
                        return m;
                    }).filter(m -> m != null).collect(Collectors.toList());
                })
                .orElse(List.of());
    }

    @Operation(summary = "删除面试历史", description = "删除指定 session 的所有面试记录")
    @DeleteMapping("/history/{sessionToken}")
    public Map<String, Object> deleteInterviewHistory(@PathVariable String sessionToken) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("未登录");
        }
        userRepository.findByUsername(auth.getName()).ifPresent(user -> {
            recordRepository.deleteBySessionToken(sessionToken);
        });
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}