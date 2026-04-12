package com.aitripplannerbackend.dto;

import com.aitripplannerbackend.utils.contents.systemContents;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE（Server-Sent Events）实时推送的事件对象。
 *
 *  整个实时推送的流程：
 * 1. 前端点击"规划" → 后端启动异步任务，返回 taskId
 * 2. 前端用 taskId 建立 SSE 连接（EventSource）
 * 3. 后端任务每完成一个步骤，就构建一个 TripStreamEvent 发送给前端
 * 4. 前端根据 type 判断是进度更新还是完成/出错
 *
 * 事件类型（type 字段）：
 *   "PROGRESS"：进度更新，前端用来显示进度条和状态文字
 *   "DONE"：全部完成，result 字段里包含完整的旅行计划
 *   "ERROR"：出错了，message 字段里包含错误信息
 *
 * {@code @JsonInclude(NON_NULL)} 表示序列化成 JSON 时跳过 null 字段，
 * 这样进度事件不会带一个 {@code "result": null}，减少传输数据量。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "SSE 推送的单条事件（data 字段 JSON 反序列化为此结构）")
public class TripStreamEvent {

    /** 任务 ID，用于关联同一次规划任务的所有事件 */
    @Schema(description = "任务 ID")
    private String taskId;

    /** 事件类型：PROGRESS / DONE / ERROR */
    @Schema(description = "事件类型", allowableValues = {"PROGRESS", "DONE", "ERROR"}, example = "PROGRESS")
    private String type;

    /** 当前阶段名称：VALIDATING / WEATHER / ATTRACTIONS / ITINERARY / ACCOMMODATION / FINALIZING / DONE / ERROR */
    @Schema(description = "当前阶段", example = "WEATHER")
    private String stage;

    /** 当前步骤序号（从 0 开始，VALIDATING=0, WEATHER=1, ATTRACTIONS=2, ITINERARY=3, ACCOMMODATION=4, FINALIZING=5） */
    @Schema(description = "当前步骤索引（从 0 起）", example = "1")
    private Integer stepIndex;

    /** 总步骤数（固定 5，定义在 systemContents.TOTAL_STEPS） */
    @Schema(description = "总步骤数", example = "5")
    private Integer totalSteps;

    /** 进度百分比 0-100，前端用来渲染进度条 */
    @Schema(description = "进度 0~100", example = "40")
    private Integer progress;

    /** 状态描述文字，如 "正在查询天气"、"景点步骤完成" */
    @Schema(description = "状态说明")
    private String message;

    /** 事件发生的时间戳 */
    @Schema(description = "事件时间（ISO-8601）")
    private Instant timestamp;

    /** 最终结果（仅 DONE 事件携带），包含完整的旅行计划 */
    @Schema(description = "仅 DONE 时携带完整 TripPlanResult")
    private TripPlanResult result;

    /**
     * 构建一个进度事件。
     * 在 TripPlannerServiceImpl 的 emitProgress 方法中调用。
     */
    public static TripStreamEvent progress(String taskId,
                                          String stage,
                                          int stepIndex,
                                          int totalSteps,
                                          Integer progress,
                                          String message) {
        return TripStreamEvent.builder()
                .taskId(taskId)
                .type("PROGRESS")
                .stage(stage)
                .stepIndex(stepIndex)
                .totalSteps(totalSteps)
                .progress(progress)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 构建一个完成事件。
     * stepIndex 和 totalSteps 都设为 TOTAL_STEPS，progress 设为 100。
     */
    public static TripStreamEvent done(String taskId, TripPlanResult result) {
        int total = systemContents.TOTAL_STEPS;
        return TripStreamEvent.builder()
                .taskId(taskId)
                .type("DONE")
                .stage("DONE")
                .stepIndex(total)
                .totalSteps(total)
                .progress(100)
                .message("行程生成完成")
                .timestamp(Instant.now())
                .result(result)
                .build();
    }
}
