package com.promptmanager.prompt_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
    info = @Info(
        title = "Prompt Management API",
        version = "1.0",
        description = "REST API for managing AI prompts.",
        contact = @Contact(
            name = "Prompt Manager Team",
            email = "support@promptmanager.com"
        ),
        license = @License(
            name = "MIT License"
        )
    )
)
public class SwaggerConfig {
}