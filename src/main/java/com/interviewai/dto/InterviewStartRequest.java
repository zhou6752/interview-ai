package com.interviewai.dto;

// InterviewStartRequest —— 开始面试的请求体
// 前端发起面试时传这个对象：
//   position：面试岗位，必填
//   sessionToken：已有的会话 token（可选，传了就用已有会话，不传就新建）
//   resumeAnalysisResult：简历 AI 分析结果（可选，传了就存入 session，面试题优先从简历题里抽）
//   mode：面试模式（practice/simulation/strict），默认 simulation
public class InterviewStartRequest {

    private String position;
    private String sessionToken;
    private ResumeAnalysisResult resumeAnalysisResult;
    private String mode = "simulation";

    public InterviewStartRequest() {
    }

    public InterviewStartRequest(String position, String sessionToken, ResumeAnalysisResult resumeAnalysisResult) {
        this.position = position;
        this.sessionToken = sessionToken;
        this.resumeAnalysisResult = resumeAnalysisResult;
    }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public ResumeAnalysisResult getResumeAnalysisResult() { return resumeAnalysisResult; }
    public void setResumeAnalysisResult(ResumeAnalysisResult resumeAnalysisResult) { this.resumeAnalysisResult = resumeAnalysisResult; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}
