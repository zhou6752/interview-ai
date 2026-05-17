package com.interviewai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.interview")
public class InterviewConfigProperties {

    private int defaultQuestionCount = 6;
    private int followUpCount = 1;
    private int evaluationBatchSize = 8;
    private int sessionTtlMinutes = 60;

    public int getDefaultQuestionCount() {
        return defaultQuestionCount;
    }

    public void setDefaultQuestionCount(int defaultQuestionCount) {
        this.defaultQuestionCount = defaultQuestionCount;
    }

    public int getFollowUpCount() {
        return followUpCount;
    }

    public void setFollowUpCount(int followUpCount) {
        this.followUpCount = followUpCount;
    }

    public int getEvaluationBatchSize() {
        return evaluationBatchSize;
    }

    public void setEvaluationBatchSize(int evaluationBatchSize) {
        this.evaluationBatchSize = evaluationBatchSize;
    }

    public int getSessionTtlMinutes() {
        return sessionTtlMinutes;
    }

    public void setSessionTtlMinutes(int sessionTtlMinutes) {
        this.sessionTtlMinutes = sessionTtlMinutes;
    }
}