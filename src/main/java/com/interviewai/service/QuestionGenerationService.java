package com.interviewai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.common.ai.PromptLoader;
import com.interviewai.config.CustomApiKeyFilter;
import com.interviewai.config.DynamicChatClientFactory;
import com.interviewai.config.InterviewConfigProperties;
import com.interviewai.dto.InterviewQuestionItem;
import com.interviewai.dto.ResumeAnalysisResult;
import com.interviewai.dto.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QuestionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationService.class);
    private final DynamicChatClientFactory chatClientFactory;
    private final InterviewConfigProperties config;
    private final ObjectMapper objectMapper;
    private final SkillService skillService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final PromptLoader promptLoader;

    public QuestionGenerationService(DynamicChatClientFactory chatClientFactory,
                                      InterviewConfigProperties config,
                                      ObjectMapper objectMapper,
                                      SkillService skillService,
                                      KnowledgeBaseService knowledgeBaseService,
                                      PromptLoader promptLoader) {
        this.chatClientFactory = chatClientFactory;
        this.config = config;
        this.objectMapper = objectMapper;
        this.skillService = skillService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.promptLoader = promptLoader;
    }

    public List<InterviewQuestionItem> generateAllQuestions(SessionContext session) {
        int mainCount = config.getDefaultQuestionCount();
        int followUpPerQuestion = config.getFollowUpCount();

        String mode = session.getMode() != null ? session.getMode() : "simulation";
        String systemPrompt = buildSystemPrompt(mode);
        String userPrompt = buildUserPrompt(session, mainCount, followUpPerQuestion, mode);

        ChatClient chatClient = chatClientFactory.createWithJsonMode(
                CustomApiKeyFilter.getApiKey(),
                CustomApiKeyFilter.getModel(),
                CustomApiKeyFilter.getBaseUrl());

        log.info("开始生成面试题: position={}, mainCount={}, followUpPerQuestion={}",
                session.getPosition(), mainCount, followUpPerQuestion);

        String aiResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        return parseQuestionResponse(aiResponse);
    }

    private String buildSystemPrompt(String mode) {
        String modeInstruction = switch (mode) {
            case "practice" -> "【练习模式】你现在是导师角色，目标是帮助候选人成长。\n" +
                "- 出题从基础概念开始，逐步递进，每道题的追问应起到引导提示作用\n" +
                "- 题面可以包含提示词，降低理解门槛\n" +
                "- 整体风格：友善、引导式、鼓励性";
            case "strict" -> "【严苛模式】你现在是高压面试官，目标是检验候选人的真实上限。\n" +
                "- 题目以 medium 和 hard 为主，减少 easy 比例\n" +
                "- 追问应层层加压，不满足于表面答案，要求候选人深挖到源码/底层\n" +
                "- 可以出一些边界场景题和架构权衡题\n" +
                "- 整体风格：严厉、专业、追根究底";
            default -> "【模拟面试模式】你现在是标准技术面试官，模拟真实面试场景。\n" +
                "- 按正常面试流程出题，难度均匀分布\n" +
                "- 追问自然衔接，模拟面试官的常规深度追问\n" +
                "- 整体风格：专业、客观、不卑不亢";
        };
        return promptLoader.loadAndFormat("prompts/question-generation-system.st", modeInstruction);
    }

    private String buildUserPrompt(SessionContext session, int mainCount, int followUpCount, String mode) {
        StringBuilder sb = new StringBuilder();

        SkillService.SkillDefinition skill = skillService.getSkill(session.getPosition());
        if (skill == null) {
            skill = skillService.getSkill("general");
        }
        if (skill == null) {
            sb.append("面试方向：").append(session.getPosition()).append("\n\n");
        } else {
            sb.append("面试方向：").append(skill.name()).append("\n");
            sb.append("说明：").append(skill.description()).append("\n\n");
            sb.append("考察范畴与权重：\n");
            for (SkillService.CategoryDefinition cat : skill.categories()) {
                sb.append("- ").append(cat.name()).append("（权重 ").append(cat.weight()).append("%）：")
                        .append(String.join("、", cat.topics())).append("\n");
            }
            sb.append("\n请按权重比例分配题目数量，确保每个范畴都有覆盖。\n\n");
        }

        // 根据模式调整难度分布
        int easyCount, mediumCount, hardCount;
        switch (mode) {
            case "practice":
                easyCount = mainCount - mainCount / 3;
                mediumCount = mainCount / 3;
                hardCount = 0;
                break;
            case "strict":
                easyCount = 0;
                mediumCount = mainCount / 2;
                hardCount = mainCount - mediumCount;
                break;
            default:
                easyCount = mainCount / 3;
                mediumCount = mainCount / 3;
                hardCount = mainCount - easyCount - mediumCount;
                break;
        }
        sb.append("生成 ").append(mainCount).append(" 道主问题。难度分布：easy ").append(easyCount)
                .append("道、medium ").append(mediumCount).append("道、hard ").append(hardCount)
                .append("道。每道主问题附带 ").append(followUpCount).append(" 条追问子问题。\n\n");

        ResumeAnalysisResult resume = session.getResumeAnalysisResult();
        if (resume != null && resume.getSummary() != null) {
            sb.append("候选人简历技术栈：").append(resume.getSummary()).append("\n");
            if (resume.getStrengths() != null && !resume.getStrengths().isEmpty()) {
                sb.append("候选人技术亮点：").append(String.join("、", resume.getStrengths())).append("\n");
            }
            sb.append("请优先围绕候选人简历中提到的技术栈出题，每道题应关联简历中的具体技能点。\n");
        }

        if (session.getUserId() != null) {
            String ragContext = knowledgeBaseService.buildRagContext(
                    session.getUserId(),
                    skill != null ? skill.name() + " " + session.getPosition() : session.getPosition()
            );
            if (!ragContext.isEmpty()) {
                sb.append("\n=== 知识库参考内容 ===\n");
                sb.append("以下是你之前上传的知识库内容，可以从中提取知识点来出题：\n\n");
                sb.append(ragContext);
                sb.append("请结合以上知识库内容出题，不要重复知识库中已有的面试题原文。\n");
            }
        }

        return sb.toString();
    }

    private List<InterviewQuestionItem> parseQuestionResponse(String aiResponse) {
        String json = aiResponse.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf("{");
            int end = json.lastIndexOf("}");
            if (start != -1 && end != -1) {
                json = json.substring(start, end + 1);
            }
        }

        try {
            List<RawQuestion> raws = objectMapper.readValue(json,
                    new TypeReference<Map<String, List<RawQuestion>>>() {
                    }).get("questions");

            List<InterviewQuestionItem> result = new ArrayList<>();
            int index = 0;
            for (RawQuestion raw : raws) {
                InterviewQuestionItem main = new InterviewQuestionItem();
                main.setIndex(index++);
                main.setQuestion(raw.question());
                main.setDifficulty(raw.difficulty() != null ? raw.difficulty() : "medium");
                main.setCategory(raw.category() != null ? raw.category() : "综合");
                main.setType(raw.type() != null ? raw.type() : "GENERAL");
                main.setParentIndex(null);
                result.add(main);

                if (raw.followUps() != null) {
                    int parentIdx = main.getIndex();
                    for (String followUp : raw.followUps()) {
                        InterviewQuestionItem fu = new InterviewQuestionItem();
                        fu.setIndex(index++);
                        fu.setQuestion(followUp);
                        fu.setDifficulty(main.getDifficulty());
                        fu.setCategory(main.getCategory());
                        fu.setType("FOLLOW_UP");
                        fu.setParentIndex(parentIdx);
                        result.add(fu);
                    }
                }
            }
            log.info("题目生成完成: 共{}题（主问题{}道，追问{}条）",
                    result.size(),
                    result.stream().filter(q -> q.getParentIndex() == null).count(),
                    result.stream().filter(q -> q.getParentIndex() != null).count());
            return result;
        } catch (Exception e) {
            log.error("AI 出题结果解析失败, 原始返回(前300字): {}",
                    aiResponse.length() > 300 ? aiResponse.substring(0, 300) : aiResponse, e);
            throw new RuntimeException("AI 出题结果解析失败：" + e.getMessage(), e);
        }
    }

    private record RawQuestion(String question, String difficulty,
                               String category, String type, List<String> followUps) {
    }
}