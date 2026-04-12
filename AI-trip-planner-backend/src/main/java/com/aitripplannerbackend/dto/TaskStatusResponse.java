package com.aitripplannerbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务状态查询响应。
 *
 * 前端可以通过 GET /api/trips/tasks/{taskId} 主动查询任务的当前状态。
 * 这是 SSE 实时推送的补充方案——如果 SSE 连接断开了，前端可以用这个接口轮询。
 *
 * 后端从 Redis 中读取任务的进度、结果、错误信息来填充这个对象。
 *
 * <h3>字段含义</h3>
 * <ul>
 *   <li>progress = 0~100：正在执行中</li>
 *   <li>progress = 100 且 result 不为 null：已完成</li>
 *   <li>progress = -1 且 error 不为 null：执行失败</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "任务状态轮询结果")
public class TaskStatusResponse {

    /** 任务 ID */
    @Schema(description = "任务 ID")
    private String taskId;

    /** 进度百分比，-1 表示出错 */
    @Schema(description = "0~100 进行中；100 且 result 非空表示完成；-1 表示失败", example = "60")
    private Integer progress;

    /** 错误信息（仅失败时有值） */
    @Schema(description = "失败时的错误说明")
    private String error;

    /** 完整的旅行计划结果（仅成功完成时有值） */
    @Schema(description = "完成后的完整行程；进行中或失败时可能为 null")
    private TripPlanResult result;
}
