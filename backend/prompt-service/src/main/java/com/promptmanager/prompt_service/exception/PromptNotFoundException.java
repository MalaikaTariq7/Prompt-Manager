package com.promptmanager.prompt_service.exception;

public class PromptNotFoundException extends RuntimeException {
    public PromptNotFoundException(String message) {
        super(message);
    }

    public PromptNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
