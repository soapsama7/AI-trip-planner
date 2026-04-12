package com.aitripplannerbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tripPlannerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI 旅行规划 API")
                        .description("""
                                异步任务规划、SSE 进度推送、任务状态轮询、草稿与正式保存、历史分页查询。
                                启动后可在 Swagger UI 中调试；SSE 端点建议在浏览器或前端用 EventSource 调用。
                                """)
                        .version("0.0.1")
                        .contact(new Contact().name("AI-trip-planner")));
    }
}
