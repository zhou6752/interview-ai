package com.interviewai.config;

import org.springframework.context.annotation.Configuration;

// RedisConfig —— Redis 配置类（当前为空壳，仅保留占位）
// StringRedisTemplate 和 ObjectMapper 由 Spring Boot 自动配置，无需手动创建 Bean
// SessionService 中直接用 StringRedisTemplate + ObjectMapper 手动序列化 JSON
@Configuration
public class RedisConfig {
}