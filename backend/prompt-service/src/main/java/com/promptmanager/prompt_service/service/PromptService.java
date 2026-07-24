package com.promptmanager.prompt_service.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.promptmanager.prompt_service.dto.PromptRequest;
import com.promptmanager.prompt_service.dto.PromptResponse;

public interface PromptService {

    PromptResponse createPrompt(PromptRequest request);

    List<PromptResponse> getAllPrompts();

    PromptResponse getPromptById(Long id);

    PromptResponse updatePrompt(Long id, PromptRequest request);

    void deletePrompt(Long id);

    PromptResponse uploadAttachment(Long promptId, MultipartFile file);

    PromptResponse deleteAttachment(Long promptId);

    List<PromptResponse> searchByTitle(String title);

    List<PromptResponse> getPromptsByCategory(String category);

    Page<PromptResponse> getPrompts(
            int page,
            int size,
            String sortBy,
            String direction);

}