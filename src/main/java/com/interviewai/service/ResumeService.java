package com.interviewai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.common.ai.PromptLoader;
import com.interviewai.config.CustomApiKeyFilter;
import com.interviewai.config.DynamicChatClientFactory;
import com.interviewai.dto.ResumeAnalysisResult;
import com.interviewai.exception.AiResponseParseException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

// ResumeService —— 简历分析服务
// 负责把简历文本发给 AI，让 AI 分析并返回结构化的分析结果
@Service  // 标记为 Service 组件，Spring 会自动管理它的生命周期
public class ResumeService {

    // ChatClient 的构造器，由 Spring 自动注入
    // 每次调用方法时用 builder 造一个新的 ChatClient
    private final DynamicChatClientFactory chatClientFactory;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    public ResumeService(DynamicChatClientFactory chatClientFactory,
                         ObjectMapper objectMapper,
                         PromptLoader promptLoader) {
        this.chatClientFactory = chatClientFactory;
        this.objectMapper = objectMapper;
        this.promptLoader = promptLoader;
    }

    // analyzeResume —— 分析简历文本，返回结构化的分析结果
    // rawText：从 PDF 提取出来的纯文本
    // 返回值：ResumeAnalysisResult 对象（包含评分、亮点、面试题等）
    public ResumeAnalysisResult analyzeResume(String rawText) {
        // 1. 用 Builder 造一个 ChatClient
        ChatClient chatClient = chatClientFactory.create(
                CustomApiKeyFilter.getApiKey(),
                CustomApiKeyFilter.getModel(),
                CustomApiKeyFilter.getBaseUrl());

        // 2. 调用 AI
        //    .system(ANALYSIS_PROMPT)  —— 告诉 AI 它的角色（面试官）和输出格式（严格 JSON）
        //    .user(rawText)             —— 把简历文本发给 AI
        //    .call()                    —— 非流式调用（等 AI 全部回答完再返回）
        //    .content()                 —— 拿到 AI 返回的字符串（应该是 JSON）
        String response = chatClient.prompt()
                .system(promptLoader.load("prompts/resume-analysis-system.st"))
                .user(rawText)
                .call()
                .content();

        // 3. 用 ObjectMapper 把 JSON 字符串转成 ResumeAnalysisResult 对象
        try {
            // readValue() 方法：把 JSON 字符串 → Java 对象
            // 第一个参数：JSON 字符串
            // 第二个参数：要转成的目标类
            return objectMapper.readValue(response, ResumeAnalysisResult.class);
        } catch (Exception e) {
            // 如果 AI 不听话，返回的 JSON 格式有问题，就会走到这里
            // 抛出 AiResponseParseException，由全局异常处理器统一处理
            throw new AiResponseParseException("AI 返回结果解析失败，请重试。原始返回：" + response, e);
        }
    }
}
