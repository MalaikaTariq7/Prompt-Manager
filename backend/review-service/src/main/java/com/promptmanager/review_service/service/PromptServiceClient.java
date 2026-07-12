package com.promptmanager.review_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.promptmanager.review_service.dto.PromptResponse;
import com.promptmanager.review_service.exception.PromptServiceException;

@Service
public class PromptServiceClient {

    private final RestTemplate restTemplate;

    @Value("${prompt.service.url}")
    private String promptServiceUrl;

    public PromptServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PromptResponse getPromptById(Long promptId) {

        try {

            String url = promptServiceUrl + "/api/prompts/" + promptId;

            ResponseEntity<PromptResponse> response =
                    restTemplate.getForEntity(url, PromptResponse.class);

            return response.getBody();

        } catch (HttpClientErrorException.NotFound ex) {

            throw new PromptServiceException(
                    "Prompt with ID " + promptId + " does not exist.");

        } catch (Exception ex) {

            throw new PromptServiceException(
                    "Unable to communicate with Prompt Service.");

        }

    }

}