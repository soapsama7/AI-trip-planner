package com.aitripplannerbackend.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 跨域（CORS）配置。
 *
 * <h3>什么是 CORS？</h3>
 * 浏览器有"同源策略"：前端页面（比如 localhost:5173）不能直接请求不同端口的后端（比如 localhost:8080）。
 * CORS 就是后端告诉浏览器"我允许这些来源访问我"，浏览器才会放行。
 *
 * <h3>为什么用 CorsWebFilter 而不是 WebMvcConfigurer？</h3>
 * 因为本项目用的是 Spring WebFlux（响应式），不是传统的 Spring MVC。
 * WebFlux 用 {@link CorsWebFilter} 来处理跨域，MVC 用 WebMvcConfigurer。
 *
 * <h3>配置项说明</h3>
 * <ul>
 *   <li>allowedOrigins：允许哪些前端地址访问（这里是 Vite 开发服务器的默认端口 5173）</li>
 *   <li>allowedMethods：允许哪些 HTTP 方法</li>
 *   <li>allowedHeaders / exposedHeaders：允许/暴露哪些请求头（"*" 表示全部）</li>
 *   <li>allowCredentials：是否允许带 Cookie（SSE 需要）</li>
 *   <li>maxAge：预检请求（OPTIONS）的缓存时间，3600秒=1小时，避免频繁发预检</li>
 * </ul>
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
