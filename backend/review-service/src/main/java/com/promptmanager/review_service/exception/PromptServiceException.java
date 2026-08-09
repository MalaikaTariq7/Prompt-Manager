package com.promptmanager.review_service.exception;

import org.springframework.http.HttpStatus;

public class PromptServiceException extends RuntimeException {

    private final HttpStatus status;

    public PromptServiceException(String message) {
        this(message, HttpStatus.BAD_GATEWAY);
    }

    public PromptServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
