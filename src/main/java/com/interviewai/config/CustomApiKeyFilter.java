package com.interviewai.config;

import com.interviewai.entity.User;
import com.interviewai.entity.UserApiConfig;
import com.interviewai.repository.UserApiConfigRepository;
import com.interviewai.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CustomApiKeyFilter extends OncePerRequestFilter {

    private static final ThreadLocal<String> API_KEY_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> MODEL_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> BASE_URL_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> EMBEDDING_MODEL_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPPORTS_EMBEDDING_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> EMB_API_KEY_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> EMB_BASE_URL_HOLDER = new ThreadLocal<>();

    public static String getApiKey() { return API_KEY_HOLDER.get(); }
    public static String getModel() { return MODEL_HOLDER.get(); }
    public static String getBaseUrl() { return BASE_URL_HOLDER.get(); }
    public static String getEmbeddingModel() { return EMBEDDING_MODEL_HOLDER.get(); }
    public static Boolean supportsEmbedding() { return SUPPORTS_EMBEDDING_HOLDER.get(); }
    public static String getEmbApiKey() { return EMB_API_KEY_HOLDER.get(); }
    public static String getEmbBaseUrl() { return EMB_BASE_URL_HOLDER.get(); }

    public static void setApiKey(String apiKey) { API_KEY_HOLDER.set(apiKey); }
    public static void setModel(String model) { MODEL_HOLDER.set(model); }
    public static void setBaseUrl(String baseUrl) { BASE_URL_HOLDER.set(baseUrl); }
    public static void setEmbeddingModel(String m) { EMBEDDING_MODEL_HOLDER.set(m); }
    public static void setSupportsEmbedding(Boolean b) { SUPPORTS_EMBEDDING_HOLDER.set(b); }

    public static void clearAll() {
        API_KEY_HOLDER.remove();
        MODEL_HOLDER.remove();
        BASE_URL_HOLDER.remove();
        EMBEDDING_MODEL_HOLDER.remove();
        SUPPORTS_EMBEDDING_HOLDER.remove();
        EMB_API_KEY_HOLDER.remove();
        EMB_BASE_URL_HOLDER.remove();
    }

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserApiConfigRepository configRepo;

    @Autowired
    private ApiKeyEncryptionUtil encryptionUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof String username) {
            User user = userRepo.findByUsername(username).orElse(null);
            if (user != null) {
                UserApiConfig activeConfig = configRepo.findByUserIdAndIsActiveTrue(user.getId());
                if (activeConfig != null) {
                    API_KEY_HOLDER.set(encryptionUtil.decrypt(activeConfig.getApiKeyEncrypted()));
                    MODEL_HOLDER.set(activeConfig.getModelName());
                    BASE_URL_HOLDER.set(activeConfig.getBaseUrl());
                }

                // 优先用专门的 embedding 配置，否则回退聊天配置
                UserApiConfig embConfig = configRepo.findByUserIdAndUseForEmbeddingTrue(user.getId());
                if (embConfig != null) {
                    EMBEDDING_MODEL_HOLDER.set(embConfig.getEmbeddingModel());
                    SUPPORTS_EMBEDDING_HOLDER.set(embConfig.getSupportsEmbedding());
                    EMB_API_KEY_HOLDER.set(encryptionUtil.decrypt(embConfig.getApiKeyEncrypted()));
                    EMB_BASE_URL_HOLDER.set(embConfig.getBaseUrl());
                } else if (activeConfig != null) {
                    EMBEDDING_MODEL_HOLDER.set(activeConfig.getEmbeddingModel());
                    SUPPORTS_EMBEDDING_HOLDER.set(activeConfig.getSupportsEmbedding());
                    EMB_API_KEY_HOLDER.set(encryptionUtil.decrypt(activeConfig.getApiKeyEncrypted()));
                    EMB_BASE_URL_HOLDER.set(activeConfig.getBaseUrl());
                }
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            clearAll();
        }
    }
}
