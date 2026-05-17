package com.interviewai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

@Component
public class DynamicChatClientFactory {

    private static final Logger log = LoggerFactory.getLogger(DynamicChatClientFactory.class);

    public ChatClient create(String apiKey, String model, String baseUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("未配置 API 密钥，请先在设置页面配置大模型服务");
        }
        if (model == null || model.isBlank()) {
            throw new RuntimeException("未配置模型名称，请先在设置页面选择大模型服务");
        }
        String effectiveBaseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "https://api.openai.com";
        log.info("创建 ChatClient: model={}, baseUrl={}, apiKey 前4位={}...",
                model, effectiveBaseUrl,
                apiKey.length() >= 4 ? apiKey.substring(0, 4) : "****");
        OpenAiApi customApi = new OpenAiApi(effectiveBaseUrl, apiKey);
        OpenAiChatModel customModel = new OpenAiChatModel(customApi,
                OpenAiChatOptions.builder().withModel(model).withTemperature(0.7).build());
        return ChatClient.builder(customModel).build();
    }

    public ChatClient createWithJsonMode(String apiKey, String model, String baseUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("未配置 API 密钥，请先在设置页面配置大模型服务");
        }
        if (model == null || model.isBlank()) {
            throw new RuntimeException("未配置模型名称，请先在设置页面选择大模型服务");
        }
        String effectiveBaseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "https://api.openai.com";
        log.info("创建 ChatClient(JSON模式): model={}, baseUrl={}, apiKey 前4位={}...",
                model, effectiveBaseUrl,
                apiKey.length() >= 4 ? apiKey.substring(0, 4) : "****");
        OpenAiApi customApi = new OpenAiApi(effectiveBaseUrl, apiKey);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(0.7)
                .withResponseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .build();
        log.info("已启用 response_format: json_object");

        OpenAiChatModel customModel = new OpenAiChatModel(customApi, options);
        return ChatClient.builder(customModel).build();
    }
}