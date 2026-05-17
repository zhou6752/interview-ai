package com.interviewai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// ResumeAnalysisResult —— AI 分析简历后返回的完整结果
// 包含评分、亮点、不足、建议、总结，以及根据简历技术栈生成的面试题
public class ResumeAnalysisResult {

    // 技术栈综合评分，0 到 100 分
    @Schema(description = "技术栈综合评分", example = "85")
    @JsonProperty("skillScore")
    private int skillScore;

    // 简历的亮点，3 到 5 条
    @Schema(description = "简历亮点，3-5条")
    @JsonProperty("strengths")
    private List<String> strengths;

    // 简历的不足之处，3 到 5 条
    @Schema(description = "不足之处，3-5条")
    @JsonProperty("weaknesses")
    private List<String> weaknesses;

    // 改进建议，3 到 5 条
    @Schema(description = "改进建议，3-5条")
    @JsonProperty("suggestions")
    private List<String> suggestions;

    // 30 字以内的综合总结
    @Schema(description = "30字以内的综合总结", example = "Java 开发基础扎实，项目经验丰富")
    @JsonProperty("summary")
    private String summary;

    // ⭐ 核心亮点：根据简历技术栈生成的定向面试题
    @Schema(description = "根据简历技术栈生成的定向面试题，3-5道")
    @JsonProperty("interviewQuestions")
    private List<ResumeQuestion> interviewQuestions;

    // 无参构造 —— Jackson 反序列化时需要
    public ResumeAnalysisResult() {
    }

    // ===== Getter / Setter =====
    // Jackson 通过 getter 把 Java 对象转成 JSON（序列化）
    // 通过 setter 把 JSON 转成 Java 对象（反序列化）

    public int getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(int skillScore) {
        this.skillScore = skillScore;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<ResumeQuestion> getInterviewQuestions() {
        return interviewQuestions;
    }

    public void setInterviewQuestions(List<ResumeQuestion> interviewQuestions) {
        this.interviewQuestions = interviewQuestions;
    }
}
