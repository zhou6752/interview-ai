package com.interviewai.dto;

import java.io.Serializable;

// QAPair —— 一轮问答的记录
// Q = Question（AI 出的题），A = Answer（用户的回答）
// 一对问答对，存在 SessionContext 的 history 列表里
// 实现了 Serializable 接口，因为要存到 Redis 里需要能序列化（转成字节/JSON）
public class QAPair implements Serializable {

    private static final long serialVersionUID = 1L;

    private String question;       // 面试题内容（AI 出的题）
    private String userAnswer;     // 用户对这道题的作答
    private String aiComment;      // AI 对这个回答的点评
    private String referenceAnswer; // 参考答案要点

    // 无参构造：Jackson 反序列化时需要（Redis 取出来转对象时会调用）
    public QAPair() {
    }

    // 全参构造：方便在代码里直接 new 出来
    public QAPair(String question, String userAnswer, String aiComment) {
        this.question = question;
        this.userAnswer = userAnswer;
        this.aiComment = aiComment;
    }

    // Getter / Setter —— Java Bean 规范，Jackson 通过它们读写字段
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getAiComment() { return aiComment; }
    public void setAiComment(String aiComment) { this.aiComment = aiComment; }

    public String getReferenceAnswer() { return referenceAnswer; }
    public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
}