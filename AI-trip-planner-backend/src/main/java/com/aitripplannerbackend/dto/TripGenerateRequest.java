package com.aitripplannerbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前端提交的旅行规划请求。
 *
 * 用户在首页填写城市、出行时间、预算后点击"规划"，
 * 前端会把这三个字段 POST 到后端，后端用这个类接收。
 *
 * <h3>校验注解说明</h3>
 * <ul>
 *   <li>{@code @NotBlank}：不能为 null 且不能全是空格（用于 String）</li>
 *   <li>{@code @NotNull}：不能为 null（用于对象类型如 BigDecimal）</li>
 *   <li>{@code @DecimalMin}：数值最小值校验，inclusive=false 表示必须严格大于 0</li>
 * </ul>
 *
 * 这些校验注解需要配合 Controller 方法参数上的 {@code @Valid} 才能生效。
 * 如果校验不通过，Spring 会自动返回 400 错误。
 *
 * <h3>Lombok 注解说明</h3>
 * <ul>
 *   <li>{@code @Data}：自动生成 getter/setter/toString/equals/hashCode</li>
 *   <li>{@code @NoArgsConstructor}：无参构造（JSON 反序列化需要）</li>
 *   <li>{@code @AllArgsConstructor}：全参构造</li>
 *   <li>{@code @Builder}：支持链式构建对象，如 {@code TripGenerateRequest.builder().city("北京").build()}</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "TripGenerateRequest", description = "启动旅行规划任务的请求体")
public class TripGenerateRequest {

    /** 目的地城市，如 "北京"、"昆明" */
    @NotBlank(message = "city 不能为空")
    @Schema(description = "目的地城市", example = "北京", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    /** 出行时间段，格式为 "yyyy-MM-dd~yyyy-MM-dd"，如 "2026-04-10~2026-04-13" */
    @NotBlank(message = "travelTime 不能为空")
    @Schema(description = "出行时间段", example = "2026-04-10~2026-04-13", requiredMode = Schema.RequiredMode.REQUIRED)
    private String travelTime;

    /** 预算金额（元），用 BigDecimal 避免浮点精度问题 */
    @NotNull(message = "budget 不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "budget 必须大于 0")
    @Schema(description = "预算（元），须大于 0", example = "5000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal budget;
}
