package com.interviewai.controller;

import com.interviewai.config.ApiKeyEncryptionUtil;
import com.interviewai.config.DynamicChatClientFactory;
import com.interviewai.config.ProviderConfigProperties;
import com.interviewai.dto.ApiConfigRequest;
import com.interviewai.entity.User;
import com.interviewai.entity.UserApiConfig;
import com.interviewai.repository.UserApiConfigRepository;
import com.interviewai.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@Tag(name = "模型配置", description = "管理用户自定义的大模型 API 密钥和模型服务")
public class ApiConfigController {

    private static final Logger log = LoggerFactory.getLogger(ApiConfigController.class);

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserApiConfigRepository configRepo;

    @Autowired
    private ApiKeyEncryptionUtil encryptionUtil;

    @Autowired
    private DynamicChatClientFactory chatClientFactory;

    @Autowired
    private ProviderConfigProperties providerConfig;

    @GetMapping("/providers")
    @Operation(summary = "获取预设大模型平台列表", description = "返回支持的平台及其默认模型（公开接口，无需登录）")
    public List<ProviderConfigProperties.ProviderConfig> getProviders() {
        return providerConfig.getProviders();
    }

    @GetMapping("/models")
    @Operation(summary = "获取支持的模型列表")
    public List<Map<String, String>> getSupportedModels() {
        List<Map<String, String>> models = new ArrayList<>();
        addModel(models, "通义千问 Turbo", "qwen-turbo",
                "https://dashscope.aliyuncs.com/compatible-mode");
        addModel(models, "通义千问 Plus", "qwen-plus",
                "https://dashscope.aliyuncs.com/compatible-mode");
        addModel(models, "DeepSeek V3", "deepseek-chat",
                "https://api.deepseek.com");
        addModel(models, "DeepSeek R1", "deepseek-reasoner",
                "https://api.deepseek.com");
        addModel(models, "GPT-4o", "gpt-4o",
                "https://api.openai.com");
        return models;
    }

    private void addModel(List<Map<String, String>> list, String name, String model, String baseUrl) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("model", model);
        m.put("baseUrl", baseUrl);
        list.add(m);
    }

    @PostMapping("/save")
    @Operation(summary = "保存 API 密钥配置")
    public Map<String, Object> saveConfig(@RequestBody ApiConfigRequest req) {
        User user = getCurrentUser();

        if (req.getConfigName() == null || req.getConfigName().isBlank()) {
            throw new RuntimeException("配置名称不能为空");
        }
        if (req.getApiKey() == null || req.getApiKey().isBlank()) {
            throw new RuntimeException("API 密钥不能为空");
        }
        if (req.getModelName() == null || req.getModelName().isBlank()) {
            throw new RuntimeException("模型名称不能为空");
        }

        UserApiConfig config = new UserApiConfig();
        config.setUserId(user.getId());
        config.setConfigName(req.getConfigName().trim());
        config.setModelName(req.getModelName().trim());
        config.setEmbeddingModel(req.getEmbeddingModel() != null ? req.getEmbeddingModel().trim() : null);
        config.setSupportsEmbedding(req.getSupportsEmbedding() != null ? req.getSupportsEmbedding() : false);
        config.setEmbeddingDimensions(req.getEmbeddingDimensions());
        config.setBaseUrl(req.getBaseUrl() != null ? req.getBaseUrl().trim() : "");
        try {
            config.setApiKeyEncrypted(encryptionUtil.encrypt(req.getApiKey().trim()));
        } catch (Exception e) {
            log.error("API 密钥加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密失败，请重试");
        }
        config.setIsActive(false);

        try {
            configRepo.save(config);
        } catch (Exception e) {
            log.error("保存配置到数据库失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存配置失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "配置已保存");
        result.put("id", config.getId());
        return result;
    }

    @GetMapping("/list")
    @Operation(summary = "获取当前用户的所有配置")
    public List<Map<String, Object>> listConfigs() {
        User user = getCurrentUser();
        List<UserApiConfig> configs = configRepo.findByUserId(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserApiConfig c : configs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("configName", c.getConfigName());
            m.put("modelName", c.getModelName());
            m.put("embeddingModel", c.getEmbeddingModel());
            m.put("supportsEmbedding", c.getSupportsEmbedding());
            m.put("embeddingDimensions", c.getEmbeddingDimensions());
            m.put("baseUrl", c.getBaseUrl());
            m.put("isActive", c.getIsActive());
            String maskedKey = maskApiKey(encryptionUtil.decrypt(c.getApiKeyEncrypted()));
            m.put("apiKeyMasked", maskedKey);
            result.add(m);
        }
        return result;
    }

    @PostMapping("/set-active/{id}")
    @Operation(summary = "设置某个配置为活跃")
    public Map<String, Object> setActive(@PathVariable Long id) {
        User user = getCurrentUser();
        UserApiConfig config = configRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("配置不存在"));
        if (!config.getUserId().equals(user.getId())) {
            throw new RuntimeException("无权操作此配置");
        }
        configRepo.deactivateAllForUser(user.getId());
        config.setIsActive(true);
        configRepo.save(config);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已切换为：" + config.getConfigName());
        return result;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    public Map<String, Object> deleteConfig(@PathVariable Long id) {
        User user = getCurrentUser();
        UserApiConfig config = configRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("配置不存在"));
        if (!config.getUserId().equals(user.getId())) {
            throw new RuntimeException("无权操作此配置");
        }
        configRepo.delete(config);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已删除");
        return result;
    }

    @GetMapping("/active")
    @Operation(summary = "查询当前活跃配置状态")
    public Map<String, Object> getActiveConfig() {
        User user = getCurrentUser();
        UserApiConfig config = configRepo.findByUserIdAndIsActiveTrue(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("hasActive", config != null);
        if (config != null) {
            result.put("configName", config.getConfigName());
            result.put("modelName", config.getModelName());
            result.put("baseUrl", config.getBaseUrl());
        }
        return result;
    }

    @PostMapping("/set-embedding/{id}")
    @Operation(summary = "设为 Embedding 专用 provider")
    public Map<String, Object> setEmbeddingProvider(@PathVariable Long id) {
        User user = getCurrentUser();
        // 清除旧的
        configRepo.findByUserIdAndUseForEmbeddingTrue(user.getId());
        List<UserApiConfig> all = configRepo.findByUserId(user.getId());
        for (UserApiConfig c : all) {
            if (c.getUseForEmbedding() != null && c.getUseForEmbedding()) {
                c.setUseForEmbedding(false);
                configRepo.save(c);
            }
        }
        UserApiConfig config = configRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("配置不存在"));
        if (!config.getUserId().equals(user.getId())) throw new RuntimeException("无权操作");
        config.setUseForEmbedding(true);
        configRepo.save(config);
        return Map.of("success", true, "message", "已设为 Embedding 专用 provider");
    }

    @GetMapping("/embedding-active")
    @Operation(summary = "获取当前 Embedding provider")
    public Map<String, Object> getEmbeddingProvider() {
        User user = getCurrentUser();
        UserApiConfig config = configRepo.findByUserIdAndUseForEmbeddingTrue(user.getId());
        if (config == null) {
            return Map.of("hasEmbedding", false);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("hasEmbedding", true);
        result.put("configName", config.getConfigName());
        result.put("modelName", config.getModelName());
        result.put("supportsEmbedding", config.getSupportsEmbedding());
        return result;
    }

    @PostMapping("/test")
    @Operation(summary = "测试连接")
    public Map<String, Object> testConnection(@RequestBody ApiConfigRequest req) {
        if (req.getApiKey() == null || req.getApiKey().isBlank()) {
            throw new RuntimeException("API 密钥不能为空");
        }
        try {
            ChatClient client = chatClientFactory.create(
                    req.getApiKey().trim(),
                    req.getModelName(),
                    req.getBaseUrl());
            String reply = client.prompt().user("回复OK").call().content();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "连接成功，模型回复: " + reply);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
            return result;
        }
    }

    @GetMapping("/status")
    @Operation(summary = "获取服务状态概览", description = "同时返回聊天配置和向量化配置的摘要状态")
    public Map<String, Object> getServiceStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Map.of("configured", false);
        }
        String username = (String) auth.getPrincipal();
        return userRepo.findByUsername(username).map(user -> {
            Map<String, Object> status = new HashMap<>();

            UserApiConfig chatConfig = configRepo.findByUserIdAndIsActiveTrue(user.getId());
            status.put("chatConfigured", chatConfig != null);
            status.put("chatModel", chatConfig != null ? chatConfig.getModelName() : null);
            status.put("chatConfigName", chatConfig != null ? chatConfig.getConfigName() : null);
            status.put("chatSupportsEmbedding", chatConfig != null && Boolean.TRUE.equals(chatConfig.getSupportsEmbedding()));

            UserApiConfig embConfig = configRepo.findByUserIdAndUseForEmbeddingTrue(user.getId());
            boolean embDedicated = embConfig != null;
            String embModel = null;
            String embConfigName = null;
            if (embDedicated) {
                embModel = embConfig.getEmbeddingModel();
                embConfigName = embConfig.getConfigName();
            } else if (chatConfig != null && Boolean.TRUE.equals(chatConfig.getSupportsEmbedding())) {
                embModel = chatConfig.getEmbeddingModel();
                embConfigName = chatConfig.getConfigName();
            }
            status.put("embConfigured", embModel != null);
            status.put("embModel", embModel);
            status.put("embConfigName", embConfigName);
            status.put("embDedicated", embDedicated);

            status.put("configured", chatConfig != null);
            return status;
        }).orElse(Map.of("configured", false));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("未登录");
        }
        String username = (String) auth.getPrincipal();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() <= 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
