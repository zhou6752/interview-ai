package com.interviewai.dto;

public class ResumeTaskStatus {

    private String taskId;
    private String status;
    private String resultJson;
    private int attempt;
    private String errorMsg;

    public ResumeTaskStatus() {
    }

    public ResumeTaskStatus(String taskId, String status, String resultJson, int attempt, String errorMsg) {
        this.taskId = taskId;
        this.status = status;
        this.resultJson = resultJson;
        this.attempt = attempt;
        this.errorMsg = errorMsg;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public int getAttempt() { return attempt; }
    public void setAttempt(int attempt) { this.attempt = attempt; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
}