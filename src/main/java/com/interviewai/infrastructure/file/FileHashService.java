package com.interviewai.infrastructure.file;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 文件哈希去重服务
 */
@Service
public class FileHashService {

    /**
     * 计算文件的 SHA-256 哈希
     */
    public String calculateHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("文件哈希计算失败", e);
        }
    }

    /**
     * 计算文本的 SHA-256 哈希（用于知识库内容去重）
     */
    public String calculateHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 必然可用
            throw new RuntimeException(e);
        }
    }
}
