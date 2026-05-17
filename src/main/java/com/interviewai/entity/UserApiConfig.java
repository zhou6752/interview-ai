package com.interviewai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_api_config")
public class UserApiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "config_name", nullable = false, length = 50)
    private String configName;

    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    @Column(name = "embedding_model", length = 50)
    private String embeddingModel;

    @Column(name = "supports_embedding")
    private Boolean supportsEmbedding = false;

    @Column(name = "embedding_dimensions")
    private Integer embeddingDimensions;

    @Column(nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "api_key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "use_for_embedding")
    private Boolean useForEmbedding = false;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }

    public UserApiConfig() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

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

    public String getApiKeyEncrypted() { return apiKeyEncrypted; }
    public void setApiKeyEncrypted(String apiKeyEncrypted) { this.apiKeyEncrypted = apiKeyEncrypted; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Boolean getUseForEmbedding() { return useForEmbedding; }
    public void setUseForEmbedding(Boolean useForEmbedding) { this.useForEmbedding = useForEmbedding; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
