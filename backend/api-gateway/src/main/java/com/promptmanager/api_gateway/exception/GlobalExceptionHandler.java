package com.promptmanager.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            WebRequest request) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();

        // Downstream service unavailable
        if (message != null &&
            (message.contains("Connection refused")
             || message.contains("connect timed out")
             || message.contains("No instances available"))) {

            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Requested service is temporarily unavailable.";
        }

        ErrorResponse error = new ErrorResponse(
                status.value(),
                message,
                request.getDescription(false).replace("uri=", "")
        );

        return new ResponseEntity<>(error, status);
    }
}