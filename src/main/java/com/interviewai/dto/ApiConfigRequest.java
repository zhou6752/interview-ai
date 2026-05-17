package com.interviewai.dto;

public class ApiConfigRequest {
    private String configName;
    private String modelName;
    private String embeddingModel;
    private Boolean supportsEmbedding;
    private Integer embeddingDimensions;
    private String baseUrl;
    private String apiKey;

    public ApiConfigRequest() {}

    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public Boolean getSupportsEmbedding() { return supportsEmbedding; }
    public void setSupportsEmbedding(Boolean supportsEmbedding) { this.supportsEmbedding = supportsEmbedding; }

    public Integer getEmbeddingDimensions() { return embeddingDimensions; }
    public void setEmbeddingDimensions(Integer embeddingDimensions) { this.embeddingDimensions = embeddingDimensions; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
