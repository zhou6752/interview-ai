package com.interviewai.dto;

import java.util.ArrayList;
import java.util.List;

// InterviewReport —— AI 生成的面试评估报告
// 面试结束时，把全部问答记录发给 AI，AI 返回这个结构化评分
// 包含：三维评分、总体评价、逐题点评、学习建议
public class InterviewReport {

    // 技术深度分（0-100）：知识掌握是否扎实，是否深入原理（权重 40%）
    private int technicalScore;
    // 逻辑表达分（0-100）：回答是否有条理，能否讲清楚（权重 25%）
    private int logicScore;
    // 知识广度分（0-100）：是否触类旁通，知识面宽不宽（权重 20%）
    private int knowledgeBreadth;
    // 实践经验分（0-100）：能否举例说明实际应用（权重 15%）
    private int practiceScore;
    // 综合分（自动计算）：technicalScore×40% + logicScore×25% + knowledgeBreadth×20% + practiceScore×15%
    private double overallScore;
    // 总体评价：AI 对整场面试的综合评语
    private String overallComment;
    // 逐题详情：每道题的情况（题目、回答、参考答案、点评）
    private List<QuestionDetail> questionDetails;
    // 学习建议：面试结束后推荐的提升方向
    private List<String> learningPath;

    public InterviewReport() {
        this.questionDetails = new ArrayList<>();
        this.learningPath = new ArrayList<>();
    }

    public int getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(int technicalScore) { this.technicalScore = technicalScore; }

    public int getLogicScore() { return logicScore; }
    public void setLogicScore(int logicScore) { this.logicScore = logicScore; }

    public int getKnowledgeBreadth() { return knowledgeBreadth; }
    public void setKnowledgeBreadth(int knowledgeBreadth) { this.knowledgeBreadth = knowledgeBreadth; }

    public int getPracticeScore() { return practiceScore; }
    public void setPracticeScore(int practiceScore) { this.practiceScore = practiceScore; }

    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }

    public String getOverallComment() { return overallComment; }
    public void setOverallComment(String overallComment) { this.overallComment = overallComment; }

    public List<QuestionDetail> getQuestionDetails() { return questionDetails; }
    public void setQuestionDetails(List<QuestionDetail> questionDetails) { this.questionDetails = questionDetails; }

    public List<String> getLearningPath() { return learningPath; }
    public void setLearningPath(List<String> learningPath) { this.learningPath = learningPath; }

    // QuestionDetail —— 单道题的评估详情
    // 每道面试题都有一个对应的详情对象，记录这题问的什么、怎么答的、参考答案、AI 点评
    public static class QuestionDetail {
        // 面试题内容
        private String question;
        // 候选人当时回答了什么
        private String userAnswer;
        // AI 给出的参考标准答案
        private String referenceAnswer;
        // AI 对此题回答的具体点评
        private String comment;

        public QuestionDetail() {
        }

        public QuestionDetail(String question, String userAnswer, String referenceAnswer, String comment) {
            this.question = question;
            this.userAnswer = userAnswer;
            this.referenceAnswer = referenceAnswer;
            this.comment = comment;
        }

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }

        public String getUserAnswer() { return userAnswer; }
        public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

        public String getReferenceAnswer() { return referenceAnswer; }
        public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
