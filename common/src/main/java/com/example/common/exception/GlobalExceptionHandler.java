package com.example.common.exception;

import com.example.common.dto.Result;
import com.example.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常: {} - {}", ex.getCode(), ex.getMessage());
        
        Result<Void> result = Result.fail(ex.getCode(), ex.getMessage());
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /**
     * 参数校验异常 - @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数校验失败: {}", errorMessage);
        
        Result<Void> result = Result.fail(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(
            BindException ex, HttpServletRequest request) {
        
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数绑定失败: {}", errorMessage);
        
        Result<Void> result = Result.fail(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("约束违反: {}", errorMessage);
        
        Result<Void> result = Result.fail(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String errorMessage = String.format("缺少必需参数: %s", ex.getParameterName());
        log.warn(errorMessage);
        
        Result<Void> result = Result.fail(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String errorMessage = String.format("参数类型错误: %s", ex.getName());
        log.warn(errorMessage);
        
        Result<Void> result = Result.fail(ErrorCode.BAD_REQUEST.getCode(), errorMessage);
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 请求体解析失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        log.warn("请求体解析失败: {}", ex.getMessage());
        
        Result<Void> result = Result.fail(ErrorCode.BAD_REQUEST.getCode(), "请求体格式错误");
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        String errorMessage = String.format("不支持%s请求方法", ex.getMethod());
        log.warn(errorMessage);
        
        Result<Void> result = Result.fail(ErrorCode.METHOD_NOT_ALLOWED.getCode(), errorMessage);
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(result);
    }

    /**
     * 文件上传大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        log.warn("文件上传大小超限: {}", ex.getMessage());
        
        Result<Void> result = Result.fail(
            ErrorCode.FILE_SIZE_EXCEEDED.getCode(), 
            ErrorCode.FILE_SIZE_EXCEEDED.getMessage()
        );
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        log.warn("资源不存在: {}", ex.getRequestURL());
        
        Result<Void> result = Result.fail(ErrorCode.NOT_FOUND);
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * 其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(
            Exception ex, HttpServletRequest request) {
        
        log.error("系统异常: ", ex);
        
        Result<Void> result = Result.fail(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(), 
            "系统繁忙，请稍后重试"
        );
        result.traceId(generateTraceId(request));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId(HttpServletRequest request) {
        return request.getRequestId();
    }
}
