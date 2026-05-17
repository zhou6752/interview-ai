package com.interviewai.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.annotation.RateLimit;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final StringRedisTemplate redis;

    // 滑动窗口限流（ZSET 实现），比固定窗口更精准，避免边界突发
    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local window = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local now = tonumber(redis.call('TIME')[1])
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            local count = redis.call('ZCARD', key)
            if count >= limit then
                return 0
            end
            redis.call('ZADD', key, now, now .. '-' .. count)
            redis.call('EXPIRE', key, window + 1)
            return 1
            """;

    public RateLimitAspect(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Around("@annotation(rateLimit)")
    public Object check(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }

        String clientIp = attrs.getRequest().getRemoteAddr();
        String key = rateLimit.key().isEmpty()
                ? joinPoint.getSignature().toShortString()
                : rateLimit.key();
        String redisKey = "rate:" + key + ":" + clientIp;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        Long allowed = redis.execute(script, List.of(redisKey),
                String.valueOf(rateLimit.seconds()), String.valueOf(rateLimit.count()));

        if (allowed == null || allowed == 0) {
            log.warn("限流触发: key={}, ip={}", key, clientIp);
            HttpServletResponse response = attrs.getResponse();
            if (response != null) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"" + rateLimit.message() + "\"}");
            }
            return null;
        }

        return joinPoint.proceed();
    }
}