package com.interviewai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

@Component
public class ApiKeyEncryptionUtil implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyEncryptionUtil.class);

    @Value("${app.encryption-key}")
    private String encryptionKey;

    @Override
    public void afterPropertiesSet() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new RuntimeException("app.encryption-key 未在 application.yml 中配置");
        }
        byte[] keyBytes = deriveKeyBytes();
        try {
            SecretKeySpec testKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, testKey);
            byte[] encrypted = cipher.doFinal("test".getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, testKey);
            byte[] decrypted = cipher.doFinal(encrypted);
            if (!"test".equals(new String(decrypted, StandardCharsets.UTF_8))) {
                throw new RuntimeException("加密模块自检失败：加解密结果不匹配");
            }
            log.info("加密模块初始化成功，密钥长度: {} 字节", keyBytes.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("加密模块启动自检失败", e);
            throw new RuntimeException("加密模块初始化失败: " + e.getMessage(), e);
        }
    }

    private byte[] deriveKeyBytes() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(digest, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }

    private SecretKeySpec getKeySpec() {
        return new SecretKeySpec(deriveKeyBytes(), "AES");
    }

    public String encrypt(String plainText) {
        try {
            SecretKeySpec key = getKeySpec();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            SecretKeySpec key = getKeySpec();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }
}