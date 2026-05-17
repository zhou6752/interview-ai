package com.interviewai.common.exception;

/**
 * 错误码体系
 */
public enum ErrorCode {

    // 通用 1xxx
    SUCCESS(0, "ok"),
    INTERNAL_ERROR(1000, "系统内部错误"),
    PARAM_INVALID(1001, "参数校验失败"),
    RATE_LIMITED(1002, "请求过于频繁，请稍后重试"),

    // 认证 2xxx
    UNAUTHORIZED(2000, "未登录或登录已过期"),
    FORBIDDEN(2001, "无权限访问"),
    TOKEN_EXPIRED(2002, "token 已过期"),

    // 简历 3xxx
    RESUME_PARSE_FAILED(3000, "简历解析失败"),
    RESUME_EMPTY(3001, "未从文件中提取到文字内容"),
    RESUME_FILE_TOO_LARGE(3002, "文件大小超过限制"),
    RESUME_UNSUPPORTED_TYPE(3003, "不支持的文件类型"),

    // 面试 4xxx
    INTERVIEW_SESSION_EXPIRED(4000, "面试会话不存在或已过期"),
    INTERVIEW_QUESTION_EMPTY(4001, "题目列表为空"),
    INTERVIEW_AI_GENERATE_FAILED(4002, "AI 出题失败"),
    INTERVIEW_AI_EVALUATE_FAILED(4003, "AI 评估失败"),

    // 知识库 5xxx
    KNOWLEDGE_BASE_UPLOAD_FAILED(5000, "知识库上传失败"),
    KNOWLEDGE_BASE_NOT_FOUND(5001, "知识库不存在"),
    KNOWLEDGE_BASE_VECTORIZE_FAILED(5002, "知识库向量化失败"),

    // AI 配置 6xxx
    PROVIDER_NOT_CONFIGURED(6000, "未配置 AI 服务"),
    PROVIDER_CONNECTION_FAILED(6001, "AI 服务连接失败"),
    PROVIDER_NO_EMBEDDING(6002, "当前服务商不支持向量化"),

    // 用户 7xxx
    USER_NOT_FOUND(7000, "用户不存在"),
    USERNAME_EXISTS(7001, "用户名已存在"),
    LOGIN_FAILED(7002, "用户名或密码错误");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}
