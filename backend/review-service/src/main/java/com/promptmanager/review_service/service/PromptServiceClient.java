package com.promptmanager.review_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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

    public PromptResponse getPromptById(Long promptId) {

        String url =
                promptServiceUrl + "/api/prompts/" + promptId;

        String authorizationHeader =
                httpServletRequest.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            throw new PromptServiceException(
                    "Authorization token is missing."
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
                        "Prompt Service returned an empty response."
                );
            }

            return prompt;

        } catch (HttpClientErrorException.NotFound exception) {

            throw new PromptServiceException(
                    "Prompt with ID "
                            + promptId
                            + " does not exist."
            );

        } catch (HttpClientErrorException.Unauthorized exception) {

            throw new PromptServiceException(
                    "Prompt Service rejected the JWT token."
            );

        } catch (HttpClientErrorException.Forbidden exception) {

            throw new PromptServiceException(
                    "Access to Prompt Service was forbidden."
            );

        } catch (ResourceAccessException exception) {

            throw new PromptServiceException(
                    "Prompt Service is unavailable."
            );

        } catch (PromptServiceException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new PromptServiceException(
                    "Unable to communicate with Prompt Service."
            );
        }
    }
}