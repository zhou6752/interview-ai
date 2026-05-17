package com.interviewai.exception;

// AiResponseParseException —— AI 返回结果解析失败时抛出的异常
// 比如 AI 没有按 Prompt 要求返回 JSON，或者 JSON 格式错误
// 继承 RuntimeException，由全局异常处理器统一捕获
public class AiResponseParseException extends RuntimeException {

    // 构造方法：传入错误信息和 AI 原始返回的内容
    // 这样全局异常处理器可以把原始返回一并记录下来，方便排查
    public AiResponseParseException(String message) {
        super(message);
    }

    // 构造方法：传入错误信息、原始异常、AI 原始返回
    public AiResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
