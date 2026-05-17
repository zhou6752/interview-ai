package com.interviewai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.config.InterviewConfigProperties;
import com.interviewai.dto.SessionContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SessionService {

    private static final String KEY_PREFIX = "session:";
    private static final String LOCK_PREFIX = "lock:answer:";
    private static final long LOCK_TTL_SECONDS = 30;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final long ttlMinutes;

    public SessionService(StringRedisTemplate stringRedisTemplate,
                           ObjectMapper objectMapper,
                           InterviewConfigProperties interviewConfig) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.ttlMinutes = interviewConfig.getSessionTtlMinutes();
    }

    // 【1/5】开始一场新的面试（不传简历分析结果）
    public SessionContext startSession(String position) {
        SessionContext context = new SessionContext();
        context.setToken(UUID.randomUUID().toString());
        context.setPosition(position);
        context.setCurrentDifficulty("easy");

        String key = KEY_PREFIX + context.getToken();
        saveToRedis(key, context, ttlMinutes);

        return context;
    }

    // 【1.1/5】开始一场新的面试（带简历分析结果）
    // 用户先上传简历让 AI 分析，再开始面试时调用这个
    // resumeResult 里带着 AI 根据简历生成的定向面试题
    public SessionContext startSession(String position, com.interviewai.dto.ResumeAnalysisResult resumeResult) {
        // 调用上面的基础方法创建会话
        SessionContext context = startSession(position);
        // 把简历分析结果存到会话中，InterviewService 出题时会用到
        context.setResumeAnalysisResult(resumeResult);
        // 更新 Redis（把 resumeAnalysisResult 也写进去）
        updateSession(context.getToken(), context);
        return context;
    }

    // 【2/5】根据 token 获取面试会话
    public SessionContext getSession(String token) {
        String key = KEY_PREFIX + token;
        return loadFromRedis(key);
    }

    // 【3/5】更新面试会话（重置 TTL，每次操作续期）
    public void updateSession(String token, SessionContext context) {
        String key = KEY_PREFIX + token;
        saveToRedis(key, context, ttlMinutes);
    }

    // 获取 session 剩余 TTL（秒），-1 表示不存在
    public long getSessionTTLSeconds(String token) {
        String key = KEY_PREFIX + token;
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }

    // 获取答题互斥锁（SETNX），防止同一 session 并发答题
    public boolean acquireAnswerLock(String token) {
        String key = LOCK_PREFIX + token;
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(key, "1", LOCK_TTL_SECONDS, TimeUnit.SECONDS));
    }

    // 释放答题互斥锁
    public void releaseAnswerLock(String token) {
        stringRedisTemplate.delete(LOCK_PREFIX + token);
    }

    // 私有方法：把 SessionContext 序列化成 JSON 字符串，存入 Redis
    private void saveToRedis(String key, SessionContext context, long ttlMinutes) {
        try {
            String json = objectMapper.writeValueAsString(context);
            stringRedisTemplate.opsForValue().set(key, json, ttlMinutes, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("SessionContext 序列化失败", e);
        }
    }

    // 私有方法：从 Redis 取出 JSON 字符串，反序列化成 SessionContext
    private SessionContext loadFromRedis(String key) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SessionContext.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 中的会话数据解析失败", e);
        }
    }
}