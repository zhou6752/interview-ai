package com.interviewai.exception;

// PdfParseException —— PDF 解析失败时抛出的异常
// 比如 PDF 加密了、文件损坏了、里面没有文字内容
// 继承 RuntimeException，由全局异常处理器统一捕获，返回友好的错误信息
public class PdfParseException extends RuntimeException {

    // 构造方法：传入错误信息
    public PdfParseException(String message) {
        super(message);
    }

    // 构造方法：传入错误信息和原始异常
    // cause 是导致这个异常的"元凶"，方便排查问题
    public PdfParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
