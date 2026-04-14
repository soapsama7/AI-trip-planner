package com.aitripplannerbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全局统一响应包装。
 *
 * <p>所有 REST 接口（SSE 流除外）均返回此结构，前端只需一套解析逻辑：
 * <pre>{@code
 * {
 *   "code": 200,
 *   "message": "ok",
 *   "data": { ... }
 * }
 * }</pre>
 *
 * <h3>约定</h3>
 * <ul>
 *   <li>{@code code = 200}：业务成功</li>
 *   <li>{@code code != 200}：业务失败，message 携带原因</li>
 * </ul>
 *
 * @param <T> data 字段的具体类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应格式")
public class Result<T> {

    @Schema(description = "状态码，200 表示成功", example = "200")
    private int code;

    @Schema(description = "提示信息", example = "ok")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "ok", data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
