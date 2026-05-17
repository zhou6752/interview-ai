package com.interviewai.controller;

import com.interviewai.common.result.Result;
import com.interviewai.common.exception.BusinessException;
import com.interviewai.common.exception.ErrorCode;
import com.interviewai.entity.User;
import com.interviewai.repository.UserRepository;
import com.interviewai.service.KnowledgeBaseService;
import com.interviewai.util.DocumentParserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@Tag(name = "知识库", description = "上传文档构建 RAG 知识库，支持向量语义搜索")
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final UserRepository userRepository;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
                                    UserRepository userRepository) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadDocument(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "name", required = false) String name,
                                                       @RequestParam(value = "category", required = false) String category) {
        Long userId = getCurrentUserId();
        String rawText = DocumentParserUtil.extractText(file);
        String docName = (name != null && !name.isBlank()) ? name : file.getOriginalFilename();
        if (docName == null) docName = "未命名文档";

        KnowledgeBaseService.UploadResult result = knowledgeBaseService.uploadDocument(
                userId, docName, rawText, file.getOriginalFilename(), file.getSize(), category);
        log.info("知识库上传: userId={}, name={}, vectorized={}, chunks={}",
                userId, docName, result.vectorized(), result.chunkCount());

        Map<String, Object> data = new HashMap<>();
        data.put("documentName", result.documentName());
        data.put("chunkCount", result.chunkCount());
        data.put("vectorized", result.vectorized());
        data.put("hint", result.hint());
        return Result.success(data);
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listDocuments() {
        Long userId = getCurrentUserId();
        return Result.success(knowledgeBaseService.getDocumentList(userId));
    }

    @DeleteMapping("/{documentName}")
    public Result<Void> deleteDocument(@PathVariable String documentName) {
        Long userId = getCurrentUserId();
        knowledgeBaseService.deleteDocument(userId, documentName);
        return Result.success(null);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Long userId = getCurrentUserId();
        return Result.success(knowledgeBaseService.getStats(userId));
    }

    @PostMapping("/chat")
    public SseEmitter ragChat(@RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        String question = body.getOrDefault("question", "");
        if (question.isBlank()) {
            SseEmitter err = new SseEmitter();
            err.completeWithError(new RuntimeException("问题不能为空"));
            return err;
        }
        return knowledgeBaseService.ragChatStream(userId, question);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            Long userId = userRepository.findByUsername(auth.getName())
                    .map(User::getId).orElse(null);
            if (userId == null) {
                throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户身份验证失败");
            }
            return userId;
        }
        throw new BusinessException(ErrorCode.LOGIN_FAILED, "请先登录");
    }
}