package com.aitripplannerbackend.service;

import com.aitripplannerbackend.dto.TripGenerateRequest;
import com.aitripplannerbackend.dto.TripPlanResult;
import com.aitripplannerbackend.dto.TripStreamEvent;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * 旅行规划核心服务接口。
 *
 * 编排"天气 → 景点 → 行程 → 落库"四个步骤，并通过 Redis Pub/Sub + SSE 推送进度。
 * 流程：前端先调 {@link #startTask} 拿到 taskId，再调 {@link #subscribeEvents} 订阅事件。
 */
public interface TripPlannerService {

    /**
     * 启动后台异步任务。
     * 任务会在 Reactor 的弹性线程池中执行，通过 Redis Pub/Sub 推送进度事件。
     *
     * @param request 用户的旅行规划请求
     * @return 任务 ID（UUID），前端用它来订阅事件或查询状态
     */
    String startTask(TripGenerateRequest request);

    /**
     * 订阅指定任务的实时事件流。
     * 底层通过 ReactiveRedisMessageListenerContainer 监听 Redis 频道，
     * 把收到的 JSON 反序列化成 TripStreamEvent 后以 Flux 形式返回。
     *
     * @param taskId 任务 ID
     * @return 事件流（PROGRESS → PROGRESS → ... → DONE 或 ERROR）
     */
    Flux<ServerSentEvent<TripStreamEvent>> subscribeEvents(String taskId);

    /**
     * 标记取消任务。
     *
     * @param taskId 任务 ID
     */
    void cancelTask(String taskId);

    /**
     * 保存用户编辑后的草稿到 Redis（不入库）。
     *
     * @param taskId 任务 ID
     * @param planResult 编辑后的行程
     */
    void saveDraft(String taskId, TripPlanResult planResult);

    /**
     * 用户确认后把行程正式保存到数据库。
     * 保存成功后该 taskId 进入只读态，不允许再编辑。
     *
     * @param taskId 任务 ID
     * @return true 表示本次成功保存；false 表示之前已保存过
     */
    boolean saveTrip(String taskId);
}
