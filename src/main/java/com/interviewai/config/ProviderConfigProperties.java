package com.interviewai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class ProviderConfigProperties {

    private List<ProviderConfig> providers = List.of();

    public List<ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderConfig> providers) {
        this.providers = providers;
    }

    public static class ProviderConfig {
        private String id;
        private String name;
        private String baseUrl;
        private String defaultModel;
        private List<String> models = List.of();
        private Boolean supportsEmbedding = false;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getDefaultModel() { return defaultModel; }
        public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
        public List<String> getModels() { return models; }
        public void setModels(List<String> models) { this.models = models; }
        public Boolean getSupportsEmbedding() { return supportsEmbedding; }
        public void setSupportsEmbedding(Boolean supportsEmbedding) { this.supportsEmbedding = supportsEmbedding; }
    }
}