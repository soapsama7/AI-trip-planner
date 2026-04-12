package com.aitripplannerbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启动任务的响应。
 *
 * 前端 POST /api/trips/plan 后，后端会启动一个异步任务，
 * 立刻返回这个 taskId 给前端。前端拿到 taskId 后，
 * 通过 GET /api/trips/tasks/{taskId}/events 建立 SSE 连接来接收实时进度。
 *
 * 只有一个字段 taskId，本质是 UUID 字符串。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "启动任务后的响应，含 taskId")
public class TaskStartResponse {
    /** 后端生成的任务唯一标识（UUID） */
    @Schema(description = "任务 ID，用于 SSE、轮询与后续草稿/保存", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String taskId;
}
