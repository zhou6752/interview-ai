package com.interviewai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.common.ai.PromptLoader;
import com.interviewai.config.CustomApiKeyFilter;
import com.interviewai.config.DynamicChatClientFactory;
import com.interviewai.dto.InterviewReport;
import com.interviewai.dto.QAPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final DynamicChatClientFactory chatClientFactory;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    public EvaluationService(DynamicChatClientFactory chatClientFactory,
                             ObjectMapper objectMapper,
                             PromptLoader promptLoader) {
        this.chatClientFactory = chatClientFactory;
        this.objectMapper = objectMapper;
        this.promptLoader = promptLoader;
    }

    private static final int BATCH_SIZE = 8;

    public InterviewReport evaluate(List<QAPair> history, String mode) {
        // 小批量直接评估
        if (history.size() <= BATCH_SIZE) {
            return doEvaluate(history, mode);
        }

        // 大批量分批评估
        log.info("分批评估: total={}, batch={}", history.size(), BATCH_SIZE);
        double totalTechnical = 0, totalLogic = 0, totalKnowledge = 0, totalPractice = 0;
        int batches = 0;

        for (int i = 0; i < history.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, history.size());
            List<QAPair> batch = history.subList(i, end);
            InterviewReport batchResult = doEvaluate(batch, mode);
            totalTechnical += batchResult.getTechnicalScore();
            totalLogic += batchResult.getLogicScore();
            totalKnowledge += batchResult.getKnowledgeBreadth();
            totalPractice += batchResult.getPracticeScore();
            batches++;
        }

        // 汇总：对各批次结果做二次综合
        InterviewReport summary = doEvaluate(history, mode);
        summary.setTechnicalScore((int) (totalTechnical / batches));
        summary.setLogicScore((int) (totalLogic / batches));
        summary.setKnowledgeBreadth((int) (totalKnowledge / batches));
        summary.setPracticeScore((int) (totalPractice / batches));
        summary.setOverallScore(
                summary.getTechnicalScore() * 0.4
                + summary.getLogicScore() * 0.25
                + summary.getKnowledgeBreadth() * 0.2
                + summary.getPracticeScore() * 0.15
        );
        return summary;
    }

    private InterviewReport doEvaluate(List<QAPair> history, String mode) {
        String prompt = buildEvaluationPrompt(history, mode);

        ChatClient chatClient = chatClientFactory.createWithJsonMode(
                CustomApiKeyFilter.getApiKey(),
                CustomApiKeyFilter.getModel(),
                CustomApiKeyFilter.getBaseUrl());

        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        // 结构化输出：解析失败重试一次
        try {
            InterviewReport report = parseResponse(aiResponse);
            report.setOverallScore(
                    report.getTechnicalScore() * 0.4
                    + report.getLogicScore() * 0.25
                    + report.getKnowledgeBreadth() * 0.2
                    + report.getPracticeScore() * 0.15
            );
            return report;
        } catch (Exception firstAttempt) {
            log.warn("首次解析失败，重试: {}", firstAttempt.getMessage());
            String retryResponse = chatClient.prompt()
                    .user(prompt + "\n\n⚠️ 请严格只返回 JSON，不要包含 markdown 代码块或其他文字。")
                    .call()
                    .content();
            InterviewReport report = parseResponse(retryResponse);
            report.setOverallScore(
                    report.getTechnicalScore() * 0.4
                    + report.getLogicScore() * 0.25
                    + report.getKnowledgeBreadth() * 0.2
                    + report.getPracticeScore() * 0.15
            );
            return report;
        }
    }

    private String buildEvaluationPrompt(List<QAPair> history, String mode) {
        // 模式角色前缀
        String rolePrefix = switch (mode != null ? mode : "simulation") {
            case "practice" -> "你是资深技术导师，目标是帮助候选人发现不足并提供建设性反馈。\n评分风格：宽容正向，多肯定亮点，不足用建议口吻表达。\noverallComment 应包含具体的鼓励和下一步学习方向。\n\n";
            case "strict" -> "你是高压面试官，标准对标大厂 P7+ 面试。请严格审视每个回答。\n评分风格：从严扣分，少给同情分。不够深度的一律标记为不足。\noverallComment 应直指要害，不留情面但言之有据。\n\n";
            default -> "你是资深技术面试评估专家，请根据候选人的全部回答给出客观评分和综合评估。\n\n";
        };

        // 拼装问答历史
        StringBuilder historyText = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            QAPair qa = history.get(i);
            historyText.append("第").append(i + 1).append("题：").append(qa.getQuestion()).append("\n");
            historyText.append("候选人回答：").append(qa.getUserAnswer()).append("\n\n");
        }

        return promptLoader.loadAndFormat("prompts/interview-evaluation.st", rolePrefix, historyText.toString());
    }

    private InterviewReport parseResponse(String aiResponse) {
        String json = aiResponse.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf("{");
            int end = json.lastIndexOf("}");
            if (start != -1 && end != -1) {
                json = json.substring(start, end + 1);
            }
        }
        try {
            return objectMapper.readValue(json, InterviewReport.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("AI 评估结果解析失败：" + e.getMessage(), e);
        }
    }
}