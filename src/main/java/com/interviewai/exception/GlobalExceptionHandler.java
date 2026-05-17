package com.interviewai.exception;

import com.interviewai.common.exception.BusinessException;
import com.interviewai.common.exception.ErrorCode;
import com.interviewai.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> handleMaxSize(MaxUploadSizeExceededException e) {
        return Result.error(ErrorCode.RESUME_FILE_TOO_LARGE.getCode(), "上传文件大小超过限制（最大 5MB）");
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleInvalidFileType(InvalidFileTypeException e) {
        return Result.error(ErrorCode.RESUME_UNSUPPORTED_TYPE.getCode(), e.getMessage());
    }

    @ExceptionHandler(AiResponseParseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleAiParseError(AiResponseParseException e) {
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "AI 分析结果解析失败：" + e.getMessage());
    }

    @ExceptionHandler(PdfParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handlePdfParseError(PdfParseException e) {
        return Result.error(ErrorCode.RESUME_PARSE_FAILED.getCode(), "PDF 解析失败：" + e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        HttpStatus status = resolveHttpStatus(e.getErrorCode());
        return ResponseEntity.status(status).body(Result.error(e.getCode(), e.getMessage()));
    }

    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        int code = errorCode.getCode();
        if (code >= 2000 && code < 3000) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == ErrorCode.FORBIDDEN.getCode()) {
            return HttpStatus.FORBIDDEN;
        }
        if (code >= 7000 && code < 8000) {
            if (code == ErrorCode.LOGIN_FAILED.getCode()) {
                return HttpStatus.UNAUTHORIZED;
            }
        }
        return HttpStatus.BAD_REQUEST;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleAll(Exception e) {
        log.error("未预期的异常", e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "系统内部错误：" + e.getMessage());
    }
}
