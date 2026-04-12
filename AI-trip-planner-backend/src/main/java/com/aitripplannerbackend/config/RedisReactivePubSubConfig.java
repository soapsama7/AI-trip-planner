package com.aitripplannerbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;

/**
 * Redis 响应式发布/订阅（Pub/Sub）配置。
 *
 * <h3>这个类在项目中的作用</h3>
 * 本项目的实时进度推送流程是：
 * 1. 后端任务执行时，通过 {@link org.springframework.data.redis.core.StringRedisTemplate}
 *    往 Redis 频道发布（publish）进度事件
 * 2. 前端通过 SSE 订阅进度，后端的 SSE 端点使用
 *    {@link ReactiveRedisMessageListenerContainer} 订阅（subscribe）同一个 Redis 频道，
 *    把收到的消息实时推给前端
 *
 * <h3>为什么用"响应式"版本？</h3>
 * 本项目用的是 Spring WebFlux（响应式框架），SSE 返回的是 {@code Flux<ServerSentEvent>}。
 * 响应式的 Redis 监听器可以直接返回 Flux，和 WebFlux 的流式模型完美配合。
 * 如果用传统的（阻塞式）Redis 监听器，就得自己做线程转换，麻烦很多。
 *
 * <h3>Bean 说明</h3>
 * <ul>
 *   <li>{@code ReactiveRedisMessageListenerContainer}：
 *       响应式 Redis 消息监听容器，用于订阅频道并以 Flux 形式接收消息</li>
 *   <li>{@code ReactiveStringRedisTemplate}：
 *       响应式 Redis 操作模板（本项目暂未直接使用，但注册后可供其他地方注入）</li>
 * </ul>
 */
@Configuration
public class RedisReactivePubSubConfig {

    @Bean
    public ReactiveRedisMessageListenerContainer reactiveRedisMessageListenerContainer(
            ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisMessageListenerContainer(factory);
    }

    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveStringRedisTemplate(factory);
    }
}
