package com.aitripplannerbackend.service;

import com.aitripplannerbackend.dto.TripHistoryPageResponse;

/**
 * 旅行历史记录查询服务接口。
 *
 * 前端"历史记录"页面通过 GET /api/trips/history?page=1&size=10 调用，
 * 从数据库中分页查询已生成的旅行计划。
 */
public interface TripHistoryService {

    /**
     * 分页查询历史记录。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return 分页结果（包含总条数、总页数和当前页列表）
     */
    TripHistoryPageResponse listHistory(int page, int size);
}
