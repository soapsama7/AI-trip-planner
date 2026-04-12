package com.aitripplannerbackend.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * 全局异常处理器。
 *
 * 把 {@link ResponseStatusException} 转成结构清晰的 JSON 响应体：
 * <pre>{ "status": 400, "message": "具体错误信息" }</pre>
 *
 * 如果不加这个处理器，Spring Boot 3.x 默认不会把 reason 放进响应体，
 * 前端只能看到 "Request failed with status code 400"，拿不到具体原因。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ex.getStatusCode().value());
        body.put("message", ex.getReason() != null ? ex.getReason() : "请求处理失败");
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}
