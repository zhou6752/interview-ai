package com.interviewai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// ResumeQuestion —— AI 根据简历生成的面试题（不是题库里预设的题）
// AI 分析简历后，会根据简历里的技术栈生成针对性的面试题
// 比如你简历写了 "Spring Boot"，AI 就会生成一道 Spring Boot 相关的面试题
public class ResumeQuestion {

    // 面试题的题干，比如 "请解释 Spring Boot 的自动配置原理"
    @Schema(description = "面试题的题干", example = "请解释 Spring Boot 的自动配置原理")
    @JsonProperty("question")
    private String question;

    // 这道题考察的是什么知识点，比如 "Spring Boot 自动配置、@Conditional 条件注解"
    @Schema(description = "考察的知识点", example = "Spring Boot 自动配置、@Conditional")
    @JsonProperty("expectedKnowledge")
    private String expectedKnowledge;

    // 题目难度：easy（简单）/ medium（中等）/ hard（困难）
    @Schema(description = "题目难度", example = "medium")
    @JsonProperty("difficulty")
    private String difficulty;

    // 无参构造方法 —— Jackson 反序列化 JSON 时需要调用无参构造
    public ResumeQuestion() {
    }

    // 全参构造方法 —— 方便在代码里直接 new 出来
    public ResumeQuestion(String question, String expectedKnowledge, String difficulty) {
        this.question = question;
        this.expectedKnowledge = expectedKnowledge;
        this.difficulty = difficulty;
    }

    // Getter / Setter —— Java Bean 规范，Jackson 通过它们读写字段
    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getExpectedKnowledge() {
        return expectedKnowledge;
    }

    public void setExpectedKnowledge(String expectedKnowledge) {
        this.expectedKnowledge = expectedKnowledge;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
