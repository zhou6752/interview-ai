package com.interviewai.exception;

// InvalidFileTypeException —— 文件类型不合法时抛出的异常
// 比如用户上传的不是 PDF 文件，或者文件名后缀不对
// 继承 RuntimeException，属于"非受检异常"，不需要强制 try-catch
public class InvalidFileTypeException extends RuntimeException {

    // 构造方法：传入错误信息
    public InvalidFileTypeException(String message) {
        super(message);
    }

    // 构造方法：传入错误信息和原始异常（方便排查）
    public InvalidFileTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
