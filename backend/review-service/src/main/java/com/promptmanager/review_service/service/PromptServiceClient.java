package com.promptmanager.review_service.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.promptmanager.review_service.dto.PromptResponse;
import com.promptmanager.review_service.exception.PromptServiceException;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class PromptServiceClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest httpServletRequest;

    @Value("${prompt.service.url}")
    private String promptServiceUrl;

    public PromptServiceClient(
            RestTemplate restTemplate,
            HttpServletRequest httpServletRequest) {

        this.restTemplate = restTemplate;
        this.httpServletRequest = httpServletRequest;
    }

    public PromptResponse getPromptById(String promptId) {

        String encodedPromptId = URLEncoder.encode(
                promptId,
                StandardCharsets.UTF_8
        );

        String url = promptServiceUrl + "/api/prompts/" + encodedPromptId;

        String authorizationHeader =
                httpServletRequest.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            throw new PromptServiceException(
                    "Authorization token is missing.",
                    HttpStatus.UNAUTHORIZED
            );
        }

        HttpHeaders headers = new HttpHeaders();

        headers.set(
                HttpHeaders.AUTHORIZATION,
                authorizationHeader
        );

        HttpEntity<Void> requestEntity =
                new HttpEntity<>(headers);

        try {

            ResponseEntity<PromptResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            requestEntity,
                            PromptResponse.class
                    );

            PromptResponse prompt = response.getBody();

            if (prompt == null) {
                throw new PromptServiceException(
                        "Prompt Service returned an empty response.",
                        HttpStatus.BAD_GATEWAY
                );
            }

            return prompt;

        } catch (HttpClientErrorException.BadRequest exception) {

            throw new PromptServiceException(
                    "Prompt ID must be a valid UUID.",
                    HttpStatus.BAD_REQUEST
            );

        } catch (HttpClientErrorException.NotFound exception) {

            throw new PromptServiceException(
                    "Prompt with ID "
                            + promptId
                            + " does not exist.",
                    HttpStatus.NOT_FOUND
            );

        } catch (HttpClientErrorException.Unauthorized exception) {

            throw new PromptServiceException(
                    "Prompt Service rejected the JWT token.",
                    HttpStatus.UNAUTHORIZED
            );

        } catch (HttpClientErrorException.Forbidden exception) {

            throw new PromptServiceException(
                    "Access to Prompt Service was forbidden.",
                    HttpStatus.FORBIDDEN
            );

        } catch (HttpServerErrorException exception) {

            throw new PromptServiceException(
                    "Prompt Service returned an internal error.",
                    HttpStatus.BAD_GATEWAY
            );

        } catch (ResourceAccessException exception) {

            throw new PromptServiceException(
                    "Prompt Service is unavailable.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );

        } catch (PromptServiceException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new PromptServiceException(
                    "Unable to communicate with Prompt Service.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}
