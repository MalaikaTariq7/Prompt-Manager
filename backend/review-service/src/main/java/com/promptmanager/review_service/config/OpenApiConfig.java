package com.promptmanager.review_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reviewServiceAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Review Service API")
                        .description("JSON-based Review Service for Prompt Management System")
                        .version("1.0"));
    }
}
