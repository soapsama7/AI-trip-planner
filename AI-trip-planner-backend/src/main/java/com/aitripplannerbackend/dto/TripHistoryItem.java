package com.aitripplannerbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 历史行程记录，用于"历史记录"页面展示。
 *
 * 前端通过 GET /api/trips/history 获取历史记录列表，
 * 后端从数据库 trip_record 表中查询并转换成这个 DTO。
 *
 * 和 {@link TripRecordEntity} 的区别是：
 * - Entity 的 planJson 是 JSON 字符串（数据库存的原始格式）
 * - 这里的 plan 是反序列化后的 TripPlanResult 对象（前端可以直接用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "单条历史行程摘要（含完整 plan）")
public class TripHistoryItem {

    /** 数据库自增 ID */
    @Schema(description = "记录主键")
    private Long id;

    /** 任务 ID */
    @Schema(description = "任务 ID")
    private String taskId;

    /** 目的地城市 */
    @Schema(description = "城市")
    private String city;

    /** 出行时间段 */
    @Schema(description = "出行时间段")
    private String travelTime;

    /** 用户设定的预算 */
    @Schema(description = "用户预算（元）")
    private BigDecimal budget;

    /** LLM 预估的总花费 */
    @Schema(description = "预估总花费（元）")
    private Integer totalCost;

    /** 完整的旅行计划（从数据库的 plan_json 反序列化得到） */
    @Schema(description = "完整行程 JSON 对象")
    private TripPlanResult plan;

    /** 记录创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
