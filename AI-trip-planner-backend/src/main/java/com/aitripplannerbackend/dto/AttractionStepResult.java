package com.aitripplannerbackend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 景点推荐步骤的输出结果。
 *
 * AttractionAgentServiceImpl 调用 LLM 推荐景点后，
 * 把 LLM 返回的 JSON 解析成这个对象。
 * 然后传给 ItineraryAgentServiceImpl，让 LLM 把这些景点安排到每日行程中。
 *
 * <h3>数据流向</h3>
 * LLM → AttractionAgentServiceImpl → AttractionStepResult
 *   → 传给 ItineraryAgentServiceImpl 作为行程规划的输入
 *   → 同时通过 SSE payload 发给前端展示推荐景点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttractionStepResult {

    /** 城市名称 */
    private String city;

    /** 推荐的景点列表（6-12 个） */
    private List<AttractionItem> attractions;

    /**
     * 单个推荐景点。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttractionItem {
        /** 景点名称，如 "故宫博物院" */
        private String name;

        /** 推荐理由 */
        private String reason;

        /** 建议游览时长，如 "2-3小时" */
        private String suggestedDuration;

        /** 预估门票费用（元） */
        private Integer expectedTicketCost;
    }
}
