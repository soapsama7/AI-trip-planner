package com.aitripplannerbackend.service.agent;

import com.aitripplannerbackend.dto.AttractionStepResult;
import com.aitripplannerbackend.dto.TripGenerateRequest;
import com.aitripplannerbackend.dto.TripPlanResult;
import com.aitripplannerbackend.dto.WeatherStepResult;

/**
 * 行程规划 Agent 服务接口。
 *
 * 综合前两步的结果（天气 + 景点），调用 LLM 生成完整的每日行程安排。
 * 这是旅行规划流程的第 3 步（共 4 步），也是最关键的一步。
 *
 *     为什么需要前两步的结果？
 * - 天气信息：LLM 会根据天气调整活动安排（下雨就安排室内景点）
 * - 景点列表：LLM 从中挑选并安排到每天的行程中，确保推荐的景点是真实存在的
 */
public interface ItineraryAgentService {

    /**
     * 生成完整的旅行行程。
     *
     * @param request     用户请求（城市、时间、预算）
     * @param weather     天气步骤的输出（天气预报数据）
     * @param attractions 景点步骤的输出（推荐景点列表）
     * @return 完整的旅行计划，包含每日行程、交通、费用等
     */
    TripPlanResult generateItinerary(TripGenerateRequest request,
                                    WeatherStepResult weather,
                                    AttractionStepResult attractions);
}
