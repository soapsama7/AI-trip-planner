package com.aitripplannerbackend.service.agent;

import com.aitripplannerbackend.dto.AttractionStepResult;
import com.aitripplannerbackend.dto.TripGenerateRequest;

/**
 * 景点推荐 Agent 服务接口。
 *
 * 负责根据用户的目的地城市和出行信息，推荐一些适合游览的景点。
 * 实现类 {@link impl.AttractionAgentServiceImpl} 通过调用 LLM（DeepSeek）获取推荐。
 *
 * 这是旅行规划流程的第 2 步（共 4 步）。
 */
public interface AttractionAgentService {

    /**
     * 推荐景点。
     *
     * @param request 包含城市、时间、预算的请求
     * @return 景点推荐结果，包含景点名称、理由、时长、票价
     */
    AttractionStepResult recommendAttractions(TripGenerateRequest request);
}
