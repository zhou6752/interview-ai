package com.interviewai.dto;

// InterviewAnswerRequest —— 前端提交回答时的请求体
// 前端在用户答完一道题后，把答案和面试 token 一起发给后端
public class InterviewAnswerRequest {

    // sessionToken：标识这场面试是"哪一场"
    // 后端根据这个 token 从 Redis 里找到对应的 SessionContext
    private String sessionToken;

    // userAnswer：用户对上一道题的回答内容
    private String userAnswer;

    // 无参构造 —— Jackson 反序列化时需要
    public InterviewAnswerRequest() {
    }

    // 全参构造 —— 方便测试时直接 new 出来
    public InterviewAnswerRequest(String sessionToken, String userAnswer) {
        this.sessionToken = sessionToken;
        this.userAnswer = userAnswer;
    }

    // Getter / Setter
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
}