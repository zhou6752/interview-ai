package com.interviewai.controller;

import com.interviewai.annotation.RateLimit;
import com.interviewai.common.result.Result;
import com.interviewai.dto.ResumeAnalysisResult;
import com.interviewai.dto.ResumeTaskStatus;
import com.interviewai.entity.ResumeAnalysisRecord;
import com.interviewai.entity.User;
import com.interviewai.exception.InvalidFileTypeException;
import com.interviewai.repository.ResumeAnalysisRecordRepository;
import com.interviewai.repository.UserRepository;
import com.interviewai.service.ResumeAnalysisTaskService;
import com.interviewai.service.ResumeService;
import com.interviewai.util.DocumentParserUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@Tag(name = "简历诊断", description = "上传简历文件并获取 AI 分析结果")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAnalysisTaskService taskService;
    private final ResumeAnalysisRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ResumeController(ResumeService resumeService,
                            ResumeAnalysisTaskService taskService,
                            ResumeAnalysisRecordRepository recordRepository,
                            UserRepository userRepository,
                            ObjectMapper objectMapper) {
        this.resumeService = resumeService;
        this.taskService = taskService;
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "同步分析简历", description = "上传 PDF/DOCX/DOC/TXT 简历文件，AI 同步分析后返回结构化结果")
    @RateLimit(count = 5, seconds = 120, message = "简历分析过于频繁，请稍后再试")
    @PostMapping("/analyze")
    public ResumeAnalysisResult analyzeResume(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException("上传的文件为空");
        }
        String rawText = DocumentParserUtil.extractText(file);
        ResumeAnalysisResult result = resumeService.analyzeResume(rawText);

        ResumeAnalysisRecord record = new ResumeAnalysisRecord();
        record.setUserId(getCurrentUserId());
        record.setFileName(file.getOriginalFilename());
        record.setResultJson(objectMapper.writeValueAsString(result));
        record.setSkillScore(result.getSkillScore());
        recordRepository.save(record);

        return result;
    }

    @Operation(summary = "异步分析简历", description = "上传简历文件后立即返回 taskId，前端轮询 /analyze/status/{taskId} 获取结果")
    @PostMapping("/analyze/async")
    public Map<String, String> analyzeResumeAsync(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileTypeException("上传的文件为空");
        }
        String rawText = DocumentParserUtil.extractText(file);
        String taskId = taskService.submitAnalysis(rawText);
        return Map.of("taskId", taskId);
    }

    @Operation(summary = "查询分析进度", description = "根据 taskId 轮询简历分析的进度和结果")
    @GetMapping("/analyze/status/{taskId}")
    public ResumeTaskStatus getTaskStatus(@PathVariable String taskId) {
        return taskService.getTaskStatus(taskId);
    }

    @Operation(summary = "获取简历分析历史", description = "返回当前用户的所有简历分析记录，按时间倒序")
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getHistory() {
        Long userId = getCurrentUserId();
        List<ResumeAnalysisRecord> records = recordRepository.findByUserIdOrderByCreateTimeDesc(userId);
        List<Map<String, Object>> list = records.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("fileName", r.getFileName());
            m.put("skillScore", r.getSkillScore());
            m.put("createTime", r.getCreateTime());
            m.put("resultJson", r.getResultJson());
            return m;
        }).toList();
        return Result.success(list);
    }

    @Operation(summary = "保存简历分析记录", description = "前端异步分析完成后调用此接口保存记录")
    @PostMapping("/history/save")
    public Result<Map<String, Object>> saveHistory(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        String fileName = (String) body.get("fileName");
        String resultJson = (String) body.get("resultJson");
        Integer skillScore = body.get("skillScore") != null
                ? ((Number) body.get("skillScore")).intValue() : 0;

        ResumeAnalysisRecord record = new ResumeAnalysisRecord();
        record.setUserId(userId);
        record.setFileName(fileName);
        record.setResultJson(resultJson);
        record.setSkillScore(skillScore);
        record = recordRepository.save(record);

        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("fileName", record.getFileName());
        result.put("skillScore", record.getSkillScore());
        result.put("createTime", record.getCreateTime());
        return Result.success(result);
    }

    @Operation(summary = "删除简历分析记录", description = "删除指定 ID 的分析记录，仅允许删除自己的记录")
    @Transactional
    @DeleteMapping("/history/{id}")
    public Result<Void> deleteHistory(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        recordRepository.deleteByIdAndUserId(id, userId);
        return Result.success(null);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return userRepository.findByUsername(auth.getName())
                    .map(User::getId).orElse(null);
        }
        return null;
    }
}