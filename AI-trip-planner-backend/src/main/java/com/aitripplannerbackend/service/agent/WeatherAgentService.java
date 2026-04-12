package com.aitripplannerbackend.service.agent;

import com.aitripplannerbackend.dto.TripGenerateRequest;
import com.aitripplannerbackend.dto.WeatherStepResult;

/**
 * 天气查询 Agent 服务接口。
 *
 * 负责根据用户的目的地城市和出行日期，查询对应的天气预报。
 * 实现类 {@link impl.WeatherAgentServiceImpl} 通过调用和风天气 API 获取数据。
 *
 * 这是旅行规划流程的第 1 步（共 4 步）。
 */
public interface WeatherAgentService {

    /**
     * 查询目的地的天气预报。
     *
     * @param request 包含城市和出行时间的请求
     * @return 天气结果，包含逐日天气、摘要、打包建议
     */
    WeatherStepResult generateWeather(TripGenerateRequest request);
}
