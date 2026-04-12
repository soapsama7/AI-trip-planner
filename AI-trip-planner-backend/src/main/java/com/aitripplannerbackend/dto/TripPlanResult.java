package com.aitripplannerbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 最终的旅行规划结果，包含完整的每日行程安排。
 *
 * 这是整个项目最核心的输出 DTO，由 ItineraryAgentServiceImpl 调用 LLM 生成，
 * 最终返回给前端渲染。它也会被序列化成 JSON 存入数据库（plan_json 字段）。
 *
 * 结构层级：TripPlanResult → DailyPlan（每天） → PlanItem（每个时段的活动）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "完整旅行规划结果（任务完成、历史记录、草稿保存均使用此结构）")
public class TripPlanResult {

    /** 目的地城市 */
    private String city;

    /** 出行时间段，如 "2026-04-10~2026-04-13" */
    private String travelTime;

    /** 总预算（元） */
    private Integer budget;

    /** 行程总结/概述，LLM 生成的一段话 */
    private String summary;

    /** 预估总花费（元）：各日 estimatedCost 之和（由服务端按分项与住宿重算） */
    private Integer totalEstimatedCost;

    /** 每日行程列表，每个元素代表一天的安排 */
    private List<DailyPlan> dailyPlans;

    /** 旅行贴士/建议列表，如 "建议提前预订酒店" */
    private List<String> tips;

    /** 预算偏低时的警告文案，预算正常时为 null */
    private String budgetWarning;

    /**
     * 与住宿相关的说明（如单日往返未安排住宿）；无则 null。
     */
    private String accommodationNote;

    /**
     * 单日行程计划。
     * 每天有一个主题（如"文化探索日"）、预估花费、以及具体的时段安排。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyPlan {
        /** 日期描述，如 "第1天 4月10日 周五" */
        private String day;

        /** 当天主题，如 "古都文化探索" */
        private String theme;

        /** 当天预估花费（元）：各时段 expectedCost 之和 + 当晚住宿参考价（服务端生成后写回） */
        private Integer estimatedCost;

        /** 当天的具体活动安排列表 */
        private List<PlanItem> planItems;

        /** 当晚住宿推荐；行程最后一天为离返程日，无住宿（为 null） */
        private Stay stay;
    }

    /**
     * 单日住宿信息。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Stay {
        /** 酒店名称 */
        private String name;

        /** 地址 */
        private String address;

        /** 参考每晚价格（元） */
        private Integer pricePerNight;

        /** 到当天最后行程点的距离（米） */
        private Integer distanceToLastSpot;

        /** 推荐理由 */
        private String reason;
    }

    /**
     * 单个时段的活动安排。
     * 描述了在什么时段、去什么地方、做什么活动、怎么去、花多少钱。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlanItem {
        /** 时段，如 "上午"、"下午"、"晚上" */
        private String period;

        /** 地点名称，如 "故宫博物院" */
        private String place;

        /** 活动描述，如 "参观太和殿、游览御花园" */
        private String activity;

        /** 交通方式，如 "地铁1号线" */
        private String transport;

        /** 预估花费（元） */
        private Integer expectedCost;

        /**
         * 是否需要提前预订。
         * {@code @JsonProperty} 确保 JSON 字段名为 "mustBookInAdvance"，
         * 因为 Jackson 默认把 Boolean 的 getter 处理为 "isMustBookInAdvance"，加上这个注解保持一致。
         */
        @JsonProperty("mustBookInAdvance")
        private Boolean mustBookInAdvance;
    }
}
