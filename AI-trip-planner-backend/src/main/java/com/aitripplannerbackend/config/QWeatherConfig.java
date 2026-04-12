package com.aitripplannerbackend.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * 和风天气 WebClient 配置。
 *
 * 从 application.yml 读取 qweather.api-host 和 qweather.api-key，
 * 创建一个预配置好 baseUrl 和认证头的 WebClient Bean，
 * 供 WeatherAgentServiceImpl 直接注入使用。
 */
@Configuration
public class QWeatherConfig {

    @Bean(name = "qweatherWebClient")
    public WebClient qweatherWebClient(
            @Value("${qweather.api-host}") String apiHost,
            @Value("${qweather.api-key}") String apiKey) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(45))
                .compress(true);

        String host = apiHost.trim();

        return WebClient.builder()
                .baseUrl("https://" + host)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("X-QW-Api-Key", apiKey.trim())
                .build();
    }
}
