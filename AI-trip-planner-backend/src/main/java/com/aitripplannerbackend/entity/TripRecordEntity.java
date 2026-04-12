package com.aitripplannerbackend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  旅行记录数据库实体，对应 trip_record 表。
 *  字段与数据库列的映射
 * MyBatis 会自动把下划线命名（如 task_id）映射成驼峰命名（如 taskId），
 * 前提是在 application.yml 中开启了 {@code map-underscore-to-camel-case: true}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripRecordEntity {

    /** 自增主键 */
    private Long id;

    /** 任务 ID（UUID），和前端、Redis 中的 taskId 一致 */
    private String taskId;

    /** 目的地城市 */
    private String city;

    /** 出行时间段 */
    private String travelTime;

    /** 用户预算 */
    private BigDecimal budget;

    /** 用户请求的原始 JSON（TripGenerateRequest 序列化） */
    private String requestJson;

    /** 最终行程计划的 JSON（TripPlanResult 序列化） */
    private String planJson;

    /** LLM 预估的总花费 */
    private Integer totalCost;

    /** 记录创建时间（数据库自动填充） */
    private LocalDateTime createdAt;

    /** 记录更新时间（数据库自动填充） */
    private LocalDateTime updatedAt;
}
