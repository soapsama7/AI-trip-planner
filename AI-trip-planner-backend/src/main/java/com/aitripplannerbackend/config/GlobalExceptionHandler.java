package com.aitripplannerbackend.config;

import com.aitripplannerbackend.dto.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 全局异常处理器 —— 所有异常统一返回 {@link Result} 格式。
 *
 * <pre>{@code { "code": 400, "message": "具体错误信息", "data": null }}</pre>
 *
 * 如果不加这个处理器，Spring Boot 3.x 默认不会把 reason 放进响应体，
 * 前端只能看到 "Request failed with status code 400"，拿不到具体原因。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Void>> handleResponseStatusException(
            ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        String message = ex.getReason() != null ? ex.getReason() : "请求处理失败";
        return ResponseEntity.status(ex.getStatusCode())
                .body(Result.fail(status, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(500)
                .body(Result.fail(500, "服务器内部错误"));
    }
}
