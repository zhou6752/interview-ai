package com.interviewai.dto;

// FinishInterviewRequest —— "结束面试"按钮的请求体
// 前端只需要传 sessionToken，后端就知道要结束哪场面试
public class FinishInterviewRequest {

    // 要结束的面试会话 token
    private String sessionToken;

    public FinishInterviewRequest() {
    }

    public FinishInterviewRequest(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}
