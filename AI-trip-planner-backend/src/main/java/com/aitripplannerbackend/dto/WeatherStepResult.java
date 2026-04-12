package com.aitripplannerbackend.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 天气查询步骤的输出结果。
 *
 * WeatherAgentServiceImpl 调用和风天气 API 后，把原始的天气数据
 * 加工成这个结构化的对象。它会被传递给下一步（景点推荐、行程规划）
 * 作为参考信息，也会通过 SSE 作为中间产物发给前端展示。
 *
 * 数据流向：
 * 和风天气 API → WeatherAgentServiceImpl → WeatherStepResult
 *   → 传给 ItineraryAgentServiceImpl（LLM 根据天气调整室内外安排）
 *   → 同时通过 SSE payload 发给前端展示天气信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherStepResult {

    /** 城市名称 */
    private String city;

    /** 出行时间段 */
    private String travelTime;

    /** 天气概况摘要，如 "以晴为主，气温 5°C~19°C" */
    private String forecastSummary;

    /** 打包建议列表，如 "建议带防晒霜"、"带一件薄外套" */
    private List<String> packingTips;

    /** 逐日天气详情列表 */
    private List<DailyWeather> daily;

    /**
     * 单日天气信息。
     * 从和风天气 API 的 DailyForecast 中提取并简化而来。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyWeather {
        /** 日期描述，如 "4月10日 周五" */
        private String day;

        /** 天气状况，如 "晴 / 晴"（白天/夜间） */
        private String weather;

        /** 最低温度（°C） */
        private Integer tempMin;

        /** 最高温度（°C） */
        private Integer tempMax;
    }
}
