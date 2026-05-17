package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// InterviewQuestion —— 面试题库表
// 存的是预设的面试题（跟 AI 生成的面试题不同，这些是写死的题库）
@Entity
@Table(name = "interview_question")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 题目分类：JAVA / SPRING / MYSQL / REDIS / JVM / 多线程
    @Column(nullable = false)
    private String category;

    // 面试题目
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    // 参考答案
    @Column(name = "reference_answer", columnDefinition = "TEXT")
    private String referenceAnswer;

    // 难度：easy / medium / hard
    @Column(nullable = false)
    private String difficulty;

    // 创建时间
    @Column(name = "create_time")
    private LocalDateTime createTime;

    // 在插入数据前自动设置创建时间
    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }

    // ===== 构造方法 =====
    public InterviewQuestion() {
    }

    public InterviewQuestion(String category, String question, String referenceAnswer, String difficulty) {
        this.category = category;
        this.question = question;
        this.referenceAnswer = referenceAnswer;
        this.difficulty = difficulty;
    }

    // ===== Getter / Setter =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getReferenceAnswer() { return referenceAnswer; }
    public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
