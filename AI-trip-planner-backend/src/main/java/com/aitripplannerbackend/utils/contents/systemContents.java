package com.aitripplannerbackend.utils.contents;

import java.time.Duration;

/**
 * 系统级常量。
 *
 * 把常量集中定义在这里
 * 修改时只改一处，避免到处写。
 */
public class systemContents {

    /**
     * 旅行规划的总步骤数：5 步。
     * 分别是：天气查询(1) → 景点推荐(2) → 行程生成(3) → 住宿推荐(4) → 结果整理(5)。
     *
     * 用于 TripStreamEvent 中的 totalSteps 字段，
     * 前端根据 stepIndex/totalSteps 来计算进度条。
     */
    public static final int TOTAL_STEPS = 5;

    public static final Duration CACHE_TTL = Duration.ofMinutes(30);

    public static final int MIN_NIGHTLY_PRICE = 100;
    public static final int REJECT_PER_DAY = 150;
    public static final int WARN_PER_DAY = 300;

    public static final float ACCOMMODATION_SHARE_OF_TOTAL_BUDGET = 0.35f;

    /**
     * 单项活动的预算上限（可调整），防止LLM胡乱输出
     */
    public static final int SINGLE_ITEM_COST_CAP = 500;
}
