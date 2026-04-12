package com.aitripplannerbackend.utils.contents;

/**
 * Redis 键前缀常量。
 */
public class redisContents {

    /** 存储最终旅行计划结果（JSON 字符串），value 是 TripPlanResult 的 JSON */
    public static final String RESULT_CACHE_PREFIX = "trip:task:result:";

    /** 存储任务进度（0~100 的数字字符串），-1 表示出错 */
    public static final String PROGRESS_CACHE_PREFIX = "trip:task:progress:";

    /**
     * Redis Pub/Sub 频道前缀。
     * 后端往这个频道发布 TripStreamEvent 的 JSON，
     * 前端的 SSE 端点订阅同一个频道来实时接收事件。
     *
     * 注意：Pub/Sub 的消息是"即时"的，不会被持久化。
     * 如果前端还没订阅就发了消息，消息就丢了。
     * 所以这里同时用 PROGRESS_CACHE_PREFIX 和 RESULT_CACHE_PREFIX 做了 Redis 缓存兜底。
     */
    public static final String EVENTS_CHANNEL_PREFIX = "trip:task:events:";

    /** 存储错误信息的 Redis key 前缀 */
    public static final String ERROR_CACHE_PREFIX = "trip:task:error:";

    /** 存储任务取消标记的 Redis key 前缀（1 表示用户已取消） */
    public static final String CANCELLED_CACHE_PREFIX = "trip:task:cancelled:";

    /** 存储原始请求 JSON（或用户编辑行程后覆盖的JSON），供用户手动保存行程时落库使用 */
    public static final String REQUEST_CACHE_PREFIX = "trip:task:request:";

    /** 存储是否已正式保存到数据库（1 表示已保存，不可再编辑） */
    public static final String SAVED_CACHE_PREFIX = "trip:task:saved:";
}
