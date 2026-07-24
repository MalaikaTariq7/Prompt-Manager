package com.promptmanager.prompt_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-that-is-at-least-32-chars",
        "jwt.expiration-ms=3600000",
        "auth.username=test-user",
        "auth.password=test-password",
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-key",
        "cloudinary.api-secret=test-secret"
})
class PromptServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
