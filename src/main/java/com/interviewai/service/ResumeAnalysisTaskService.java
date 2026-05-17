package com.interviewai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.config.CustomApiKeyFilter;
import com.interviewai.dto.ResumeAnalysisResult;
import com.interviewai.dto.ResumeTaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class ResumeAnalysisTaskService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisTaskService.class);
    private static final String TASK_PREFIX = "resume:task:";
    private static final String CACHE_PREFIX = "resume:cache:";
    private static final int TASK_TTL_MINUTES = 30;
    private static final int MAX_RETRIES = 3;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ResumeService resumeService;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public ResumeAnalysisTaskService(StringRedisTemplate redis,
                                      ObjectMapper objectMapper,
                                      ResumeService resumeService) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.resumeService = resumeService;
    }

    public String submitAnalysis(String rawText) {
        String contentHash = sha256(rawText);

        String cachedResult = redis.opsForValue().get(CACHE_PREFIX + contentHash);
        if (cachedResult != null) {
            log.info("简历内容命中缓存: hash={}", contentHash.substring(0, 12));
            String taskId = UUID.randomUUID().toString().substring(0, 8);
            ResumeTaskStatus status = new ResumeTaskStatus(taskId, "COMPLETED", cachedResult, 1, null);
            saveTaskStatus(taskId, status);
            return taskId;
        }

        String taskId = UUID.randomUUID().toString().substring(0, 8);
        ResumeTaskStatus status = new ResumeTaskStatus(taskId, "PENDING", null, 0, null);
        saveTaskStatus(taskId, status);

        String capturedApiKey = CustomApiKeyFilter.getApiKey();
        String capturedModel = CustomApiKeyFilter.getModel();
        String capturedBaseUrl = CustomApiKeyFilter.getBaseUrl();

        executor.submit(() -> processTask(taskId, rawText, contentHash,
                capturedApiKey, capturedModel, capturedBaseUrl));

        log.info("提交异步分析: taskId={}, contentLength={}", taskId, rawText.length());
        return taskId;
    }

    public ResumeTaskStatus getTaskStatus(String taskId) {
        String key = TASK_PREFIX + taskId;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return new ResumeTaskStatus(taskId, "NOT_FOUND", null, 0, "任务不存在或已过期");
        }
        try {
            return objectMapper.readValue(json, ResumeTaskStatus.class);
        } catch (Exception e) {
            return new ResumeTaskStatus(taskId, "ERROR", null, 0, "状态解析失败");
        }
    }

    private void processTask(String taskId, String rawText, String contentHash,
                              String apiKey, String model, String baseUrl) {
        try {
            CustomApiKeyFilter.setApiKey(apiKey);
            CustomApiKeyFilter.setModel(model);
            CustomApiKeyFilter.setBaseUrl(baseUrl);

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    updateStatus(taskId, "ANALYZING", null, attempt, null);

                    ResumeAnalysisResult result = resumeService.analyzeResume(rawText);
                    String resultJson = objectMapper.writeValueAsString(result);

                    updateStatus(taskId, "COMPLETED", resultJson, attempt, null);

                    redis.opsForValue().set(CACHE_PREFIX + contentHash, resultJson,
                            TASK_TTL_MINUTES, TimeUnit.MINUTES);

                    log.info("异步分析完成: taskId={}, attempt={}", taskId, attempt);
                    return;
                } catch (Exception e) {
                    log.warn("异步分析失败(taskId={}, attempt={}): {}", taskId, attempt, e.getMessage());
                    if (attempt == MAX_RETRIES) {
                        updateStatus(taskId, "FAILED", null, attempt, e.getMessage());
                    } else {
                        try {
                            Thread.sleep(1000L * attempt);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        } finally {
            CustomApiKeyFilter.clearAll();
        }
    }

    private void updateStatus(String taskId, String status, String resultJson,
                               int attempt, String errorMsg) {
        saveTaskStatus(taskId, new ResumeTaskStatus(taskId, status, resultJson, attempt, errorMsg));
    }

    private void saveTaskStatus(String taskId, ResumeTaskStatus status) {
        try {
            String json = objectMapper.writeValueAsString(status);
            redis.opsForValue().set(TASK_PREFIX + taskId, json, TASK_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("保存任务状态失败: taskId={}", taskId, e);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}