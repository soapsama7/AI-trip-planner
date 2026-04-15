package com.aitripplannerbackend.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * 为 Spring AI（DeepSeek LLM）配置 HTTP 客户端超时。
 *
 * <h3>为什么需要这个类？</h3>
 * Spring AI 底层通过 {@link RestClient} 和 {@link WebClient} 发送 HTTP 请求给 LLM。
 * LLM 生成回答通常需要几十秒甚至几分钟，而 Spring 默认的读超时只有 30 秒，
 * 如果不改，请求还没返回就超时报错了。
 *
 * <h3>核心思路</h3>
 * 1. 创建一个 Reactor Netty 的 {@link HttpClient}，把连接超时设为 10 秒，响应超时设为 10 分钟。
 * 2. 分别包装成 {@link RestClient.Builder}（同步调用用）和 {@link WebClient.Builder}（异步/流式调用用），
 *    加上 {@code @Primary} 让 Spring AI 自动使用它们，而不是默认的。
 *
 * <h3>什么是 @Primary？</h3>
 * Spring 容器里可能有多个同类型的 Bean（比如我们还有 QWeatherConfig 里的 WebClient），
 * {@code @Primary} 表示"如果没有明确指定用哪个，就用这个"。
 * Spring AI 注入 RestClient.Builder 时不会指定名字，所以会自动用到这个。
 */
@Configuration
public class AIRestClientConfig {

    /**
     * 创建一个为 LLM 调用定制的 Reactor Netty HttpClient。
     * <ul>
     *   <li>CONNECT_TIMEOUT_MILLIS = 10秒：TCP 连接建立超时</li>
     *   <li>responseTimeout = 10分钟：等待 LLM 响应的最大时间</li>
     * </ul>
     */
    private HttpClient llmHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofMinutes(10));
    }

    /**
     * 为 Spring AI 的同步调用提供 RestClient.Builder。
     *
     * {@link ReactorClientHttpRequestFactory} 是 Spring 6 新增的，
     * 它把 Reactor Netty 的 HttpClient 适配成 Spring RestClient 能用的请求工厂。
     * 简单说就是：Netty HttpClient → RequestFactory → RestClient.Builder。
     * &#064;Primary  的作用：让spring默认选这个（因为其它配置类也有WebClient）
     */
    @Bean
    @Primary
    public RestClient.Builder llmRestClientBuilder() {
        return RestClient.builder().requestFactory(new ReactorClientHttpRequestFactory(llmHttpClient()));
    }

    /**
     * 为 Spring AI 的流式/异步调用提供 WebClient.Builder。
     *
     * {@link ReactorClientHttpConnector} 把 Netty HttpClient 包装成 WebClient 能用的连接器。
     * &#064;Primary  的作用：让spring默认选这个
     */
    @Bean
    @Primary
    public WebClient.Builder llmWebClientBuilder() {
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(llmHttpClient()));
    }
}
