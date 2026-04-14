package com.aitripplannerbackend.service.impl;

import com.aitripplannerbackend.dto.AttractionStepResult;
import com.aitripplannerbackend.dto.BudgetCheckResult;
import com.aitripplannerbackend.dto.TripGenerateRequest;
import com.aitripplannerbackend.dto.TripPlanResult;
import com.aitripplannerbackend.dto.TripStreamEvent;
import com.aitripplannerbackend.dto.WeatherStepResult;
import com.aitripplannerbackend.entity.TripRecordEntity;
import com.aitripplannerbackend.mapper.TripRecordMapper;
import com.aitripplannerbackend.service.TripPlannerService;
import com.aitripplannerbackend.service.agent.AttractionAgentService;
import com.aitripplannerbackend.service.agent.ItineraryAgentService;
import com.aitripplannerbackend.service.agent.WeatherAgentService;
import com.aitripplannerbackend.utils.BudgetValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import static com.aitripplannerbackend.utils.contents.redisContents.*;
import static com.aitripplannerbackend.utils.contents.systemContents.CACHE_TTL;
import static com.aitripplannerbackend.utils.contents.systemContents.TOTAL_STEPS;

/**
 * 旅行规划核心编排服务 —— 整个项目最重要的类。
 *
 * <h3>职责</h3>
 * 编排"天气 → 景点 → 行程 → 落库"四个步骤，同时：
 * 1. 每完成一个步骤就通过 Redis Pub/Sub 推送进度事件给前端
 * 2. 把进度和结果缓存到 Redis（兜底，SSE 断了前端还能轮询查）
 * 3. 最终把完整结果存入 MySQL
 *
 * <h3>异步执行机制</h3>
 * 这个类不是在 Controller 的请求线程里同步执行的。
 * startTask() 方法会把 runTask() 扔到 {@link Schedulers#boundedElastic()} 线程池中异步执行。
 *
 * <h3>什么是 Schedulers.boundedElastic()？</h3>
 * 它是 Project Reactor 提供的一个弹性线程池，专门用于执行阻塞操作（如调 HTTP API、写数据库）。
 * WebFlux 的事件循环线程不能做阻塞操作（会卡住所有请求），
 * 所以把耗时任务甩给弹性线程池是标准做法。
 *
 * <h3>关键依赖</h3>
 * <ul>
 *   <li>{@link StringRedisTemplate}：同步 Redis 操作（写缓存、发 Pub/Sub）</li>
 *   <li>{@link ReactiveRedisMessageListenerContainer}：响应式 Redis 订阅（读 Pub/Sub）</li>
 *   <li>{@link ObjectMapper}：JSON 序列化/反序列化（Jackson 核心类）</li>
 *   <li>三个 Agent Service：天气、景点、行程的具体实现</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripPlannerServiceImpl implements TripPlannerService {

    private final ObjectMapper objectMapper;
    private final TripRecordMapper tripRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer;

    private final WeatherAgentService weatherAgentService;
    private final AttractionAgentService attractionAgentService;
    private final ItineraryAgentService itineraryAgentService;

    /**
     * 异步模式：启动后台任务。
     *
     * 1. 先做预算硬拒绝校验（同步），不合理直接返回 400，前端不会进入 SSE 流程
     * 2. 生成一个 UUID 作为 taskId
     * 3. 把 runTask 提交到弹性线程池异步执行
     * 4. 立刻返回 taskId 给前端（不等任务完成）
     *
     * 前端拿到 taskId 后，通过 SSE 订阅进度事件。
     */
    @Override
    public String startTask(TripGenerateRequest request) {
        BudgetCheckResult budgetCheck = BudgetValidator.check(
                request.getBudget(), request.getTravelTime());
        if (budgetCheck.isRejected()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, budgetCheck.getMessage());
        }
        String taskId = UUID.randomUUID().toString();
        cacheRequest(taskId, request);
        /*
            Schedulers.boundedElastic()返回一个 线程池（Scheduler）
            专门用于阻塞任务、线程数有上限（bounded）且会自动扩容（elastic）
            .schedule(...)提交任务执行
         */
        Schedulers.boundedElastic().schedule(() -> runTask(taskId, request, budgetCheck));
        return taskId;
    }

    /**
     * 订阅指定任务的 Redis Pub/Sub 事件流。
     *
     * <h3>调用链详解</h3>
     * <pre>
     * reactiveRedisMessageListenerContainer.receive(topic)  // 1. 监听 Redis 频道，返回 Flux<Message>
     *   .map(ReactiveSubscription.Message::getMessage（msg -> msg.getMessage()）)  // 2. 从 Message 中提取消息体（String）
     *   .map(Object::toString)                              // 3. 确保转为 String
     *   .map(json -> objectMapper.readValue(...))           // 4. JSON → TripStreamEvent 对象
     *   .filter(Objects::nonNull)                           // 5. 过滤掉 null（解析失败时的兜底值）
     * </pre>
     * <p>
     * 整个链是"响应式"的：Redis 每发一条消息，这条链就自动处理一次，
     * 处理结果通过 Flux 推给 Controller，Controller 再推给前端的 SSE。
     */
    @Override
    public Flux<ServerSentEvent<TripStreamEvent>> subscribeEvents(String taskId) {
        String channel = EVENTS_CHANNEL_PREFIX + taskId;
        // 直接receive(...) 返回的不是“纯字符串消息”，而是一个包装对象：ReactiveSubscription.Message<K, V>。因此需要getMessage()提取消息体
        Flux<TripStreamEvent> tripStreamEventFlux = reactiveRedisMessageListenerContainer
                .receive(new ChannelTopic(channel))
                .map(ReactiveSubscription.Message::getMessage)
                .map(Object::toString)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, TripStreamEvent.class);
                    } catch (Exception e) {
                        return TripStreamEvent.builder()
                                .taskId(taskId)
                                .type("ERROR")
                                .stage("SUBSCRIBE")
                                .stepIndex(0)
                                .totalSteps(TOTAL_STEPS)
                                .progress(0)
                                .message("订阅事件解析失败")
                                .build();
                    }
                })
                .filter(Objects::nonNull);

        return tripStreamEventFlux
                .map(event -> ServerSentEvent.<TripStreamEvent>builder()
                        .id(event.getTaskId())
                        .data(event)
                        .build());
    }

    @Override
    public void cancelTask(String taskId) {
        stringRedisTemplate.opsForValue()
                .set(CANCELLED_CACHE_PREFIX + taskId, "1", CACHE_TTL);
    }

    @Override
    public void saveDraft(String taskId, TripPlanResult planResult) {
        if ("1".equals(stringRedisTemplate.opsForValue().get(SAVED_CACHE_PREFIX + taskId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该行程已保存，不能再编辑");
        }
        try {
            cacheResult(taskId, planResult, 100);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "草稿内容格式不正确，请重试");
        }
    }

    @Override
    public boolean saveTrip(String taskId) {
        // 做一下二次校验，防止重复写入数据库，保障程序健壮性
        if ("1".equals(stringRedisTemplate.opsForValue().get(SAVED_CACHE_PREFIX + taskId))
                || tripRecordMapper.countByTaskId(taskId) > 0) {
            return false;
        }
        String requestJson = stringRedisTemplate.opsForValue().get(REQUEST_CACHE_PREFIX + taskId);
        String planJson = stringRedisTemplate.opsForValue().get(RESULT_CACHE_PREFIX + taskId);
        // 一样是做二次校验
        if (requestJson == null || requestJson.isBlank() || planJson == null || planJson.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到可保存的行程结果，请先完成行程生成");
        }
        try {
            TripGenerateRequest request = objectMapper.readValue(requestJson, TripGenerateRequest.class);
            TripPlanResult planResult = objectMapper.readValue(planJson, TripPlanResult.class);
            persist(taskId, request, planResult);
            stringRedisTemplate.opsForValue().set(SAVED_CACHE_PREFIX + taskId, "1", CACHE_TTL);
            return true;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "保存失败，行程数据解析异常");
        }
    }

    /**
     * 核心执行方法 —— 按顺序跑完四个步骤。
     * VALIDATING (5%)  → 校验参数
     * WEATHER   (20%)  → 调用和风天气 API 查天气 → (35%) 完成
     * ATTRACTIONS(45%) → 调用 LLM 推荐景点     → (65%) 完成
     * ITINERARY (75%)     → 调用 LLM 生成行程       → (88%) 完成
     * ACCOMMODATION (90%) → 基于高德补全每日住宿    → (95%) 完成
     * FINALIZING(97%)     → 缓存最终结果            → (100%) 全部完成
     *
     * @param taskId  任务 ID
     * @param request 用户请求表单
     */
    private void runTask(String taskId, TripGenerateRequest request, BudgetCheckResult budgetCheck) {
        try {
            emitProgress(taskId, "VALIDATING", 0, 5, "已接收请求，正在校验参数");

            emitProgress(taskId, "WEATHER", 1, 20, "正在查询天气");
            WeatherStepResult weather = weatherAgentService.generateWeather(request);
            emitProgress(taskId, "WEATHER", 1, 35, "天气步骤完成");

            emitProgress(taskId, "ATTRACTIONS", 2, 45, "正在推荐景点");
            AttractionStepResult attractions = attractionAgentService.recommendAttractions(request);
            emitProgress(taskId, "ATTRACTIONS", 2, 65, "景点步骤完成");

            emitProgress(taskId, "ITINERARY", 3, 75, "正在生成完整行程");
            TripPlanResult planResult = itineraryAgentService.generateItinerary(request, weather, attractions);
            emitProgress(taskId, "ITINERARY", 3, 88, "行程步骤完成");

            emitProgress(taskId, "ACCOMMODATION", 4, 90, "正在补充每日住宿建议");
            emitProgress(taskId, "ACCOMMODATION", 4, 95, "住宿建议补充完成");

            if (budgetCheck.isWarned()) {
                planResult.setBudgetWarning(budgetCheck.getMessage());
            }

            emitProgress(taskId, "FINALIZING", 5, 97, "正在整理最终结果");
            cacheResult(taskId, planResult, 100);
            TripStreamEvent done = TripStreamEvent.done(taskId, planResult);
            publishEvent(taskId, done);
        } catch (CancellationException ex) {
            log.info("[taskId={}] 任务已取消", taskId);
            cacheError(taskId, "任务已取消");
            TripStreamEvent cancelled = TripStreamEvent.builder()
                    .taskId(taskId)
                    .type("ERROR")
                    .stage("CANCELLED")
                    .stepIndex(0)
                    .totalSteps(TOTAL_STEPS)
                    .progress(0)
                    .message("任务已取消")
                    .build();
            publishEvent(taskId, cancelled);
        } catch (Exception ex) {
            log.error("[taskId={}] 规划失败: {}", taskId, ex.getMessage(), ex);
            String errorMsg = formatError(ex);
            TripStreamEvent err = TripStreamEvent.builder()
                    .taskId(taskId)
                    .type("ERROR")
                    .stage("ERROR")
                    .stepIndex(0)
                    .totalSteps(TOTAL_STEPS)
                    .progress(0)
                    .message(errorMsg)
                    .build();
            cacheError(taskId, errorMsg);
            publishEvent(taskId, err);
        }
    }

    /**
     * 提取异常的根因信息，用于返回给前端展示。
     * 有些异常是层层包装的（A 包 B 包 C），这个方法会一层层剥开，
     * 找到最内层的那个（根因），把外层和根因的 message 拼起来。
     * 比如："景点步骤 JSON 解析失败 | rootCause=Unexpected character..."
     * guard 计数器防止异常链形成死循环（理论上不会，但注意一下防御性编程，且一般来说10层链够长了）。
     */
    private String formatError(Throwable ex) {
        Throwable cur = ex;
        int guard = 0;
        while (cur.getCause() != null && cur.getCause() != cur && guard++ < 10) {
            cur = cur.getCause();
        }
        /**
            getSimpleName() 是 Class 上的方法，返回这个类在源码里的短名字（不含包名）
            在 formatError 里，当 getMessage() 是 null 或空时，用 getClass().getSimpleName() 代替
            这样前端至少能看到是哪种异常，而不是一片空白。
         */
        String rootMsg = (cur.getMessage() == null || cur.getMessage().isBlank()) ? cur.getClass().getSimpleName() : cur.getMessage();
        String topMsg = (ex.getMessage() == null || ex.getMessage().isBlank()) ? ex.getClass().getSimpleName() : ex.getMessage();
        /**
           如果最外层和根因说的是同一件事（或拼出来一样），只返回一句，避免重复。
           否则返回：外层摘要 + 根因细节
         */
        if (rootMsg.equals(topMsg)) {
            return rootMsg;
        }
        return topMsg + " | rootCause=" + rootMsg;
    }

    /**
     * 把错误信息缓存到 Redis，同时把进度设为 -1 表示失败。
     * 这样前端通过轮询 /tasks/{taskId} 也能发现任务失败了。
     */
    private void cacheError(String taskId, String errorMsg) {
        try {
            stringRedisTemplate.opsForValue()
                    .set(ERROR_CACHE_PREFIX + taskId, errorMsg, CACHE_TTL);
            stringRedisTemplate.opsForValue()
                    .set(PROGRESS_CACHE_PREFIX + taskId, "-1", CACHE_TTL);
        } catch (Exception e) {
            log.warn("[taskId={}] 写入错误状态到 Redis 失败", taskId, e);
        }
    }

    /**
     * 把最终结果持久化到 MySQL 的 trip_record 表。
     * 请求和结果都序列化成 JSON 存储，方便后续查询历史记录。
     *
     */
    private void persist(String taskId, TripGenerateRequest request, TripPlanResult planResult) throws JsonProcessingException {
        TripRecordEntity entity = new TripRecordEntity();
        entity.setTaskId(taskId);
        entity.setCity(request.getCity());
        entity.setTravelTime(request.getTravelTime());
        entity.setBudget(request.getBudget());
        entity.setRequestJson(objectMapper.writeValueAsString(request));
        entity.setPlanJson(objectMapper.writeValueAsString(planResult));
        entity.setTotalCost(planResult.getTotalEstimatedCost());
        tripRecordMapper.insert(entity);
    }

    /**
     * 把进度和结果缓存到 Redis。
     * 设置 30 分钟过期，避免 Redis 里的数据无限增长。
     */
    private void cacheResult(String taskId, TripPlanResult planResult, int progress) throws JsonProcessingException {
        stringRedisTemplate.opsForValue()
                .set(PROGRESS_CACHE_PREFIX + taskId, String.valueOf(progress), CACHE_TTL);
        stringRedisTemplate.opsForValue()
                .set(RESULT_CACHE_PREFIX + taskId, objectMapper.writeValueAsString(planResult), CACHE_TTL);
    }

    /**
     * 发送一个进度事件：写 Redis 进度缓存，并通过 Pub/Sub 推给已订阅 SSE 的前端。
     */
    private void emitProgress(String taskId,
                              String stage,
                              int stepIndex,
                              int progress,
                              String message) {
        // 在发进度前检查取消状态
        String cancelled = stringRedisTemplate.opsForValue().get(CANCELLED_CACHE_PREFIX + taskId);
        if ("1".equals(cancelled)) {
            throw new CancellationException("任务已取消");
        }
        stringRedisTemplate.opsForValue()
                .set(PROGRESS_CACHE_PREFIX + taskId, String.valueOf(progress), CACHE_TTL);
        TripStreamEvent event = TripStreamEvent.progress(taskId, stage, stepIndex, TOTAL_STEPS, progress, message);
        publishEvent(taskId, event);
    }

    /**
     * 存储原始请求表单项进redis
     */
    private void cacheRequest(String taskId, TripGenerateRequest request) {
        try {
            stringRedisTemplate.opsForValue()
                    .set(REQUEST_CACHE_PREFIX + taskId, objectMapper.writeValueAsString(request), CACHE_TTL);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "任务初始化失败，请重试");
        }
    }

    /**
     * 通过 Redis Pub/Sub 发布事件。
     * 用 try-catch 包裹，避免 Redis 不可用时整个任务失败（降级处理）。
     */
    private void publishEvent(String taskId, TripStreamEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            stringRedisTemplate.convertAndSend(EVENTS_CHANNEL_PREFIX + taskId, json);
        } catch (Exception e) {
            log.warn("[taskId={}] Redis Pub/Sub 发布失败: {}", taskId, e.getMessage());
        }
    }
}
