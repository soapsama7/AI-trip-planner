package com.aitripplannerbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用启动类。
 *
 * {@code @SpringBootApplication} 是一个组合注解，等价于同时加上：
 * <ul>
 *   <li>{@code @Configuration}：标记为配置类</li>
 *   <li>{@code @EnableAutoConfiguration}：开启自动配置（Spring Boot 会根据 classpath 里的依赖
 *       自动配置 Redis、MyBatis、WebFlux 等）</li>
 *   <li>{@code @ComponentScan}：自动扫描当前包及子包下的 @Component、@Service、@Controller 等</li>
 * </ul>
 *
 * main 方法是整个应用的入口，{@code SpringApplication.run()} 会启动 Spring 容器、
 * 内嵌的 Netty 服务器（WebFlux 默认用 Netty 而不是 Tomcat），然后开始监听 HTTP 请求。
 */
@SpringBootApplication
public class AiTripPlannerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiTripPlannerBackendApplication.class, args);
    }
}
