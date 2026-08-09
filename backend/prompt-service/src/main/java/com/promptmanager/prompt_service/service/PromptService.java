package com.promptmanager.prompt_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.promptmanager.prompt_service.dto.PromptRequest;
import com.promptmanager.prompt_service.dto.PromptResponse;

public interface PromptService {

    PromptResponse createPrompt(PromptRequest request);

    List<PromptResponse> getAllPrompts();

    PromptResponse getPromptById(UUID id);

    PromptResponse updatePrompt(UUID id, PromptRequest request);

    void deletePrompt(UUID id);

    boolean promptExists(UUID id);

    PromptResponse uploadAttachment(UUID promptId, MultipartFile file);

    PromptResponse deleteAttachment(UUID promptId);

    List<PromptResponse> searchByTitle(String title);

    List<PromptResponse> getPromptsByCategory(String category);

    Page<PromptResponse> getPrompts(
            int page,
            int size,
            String sortBy,
            String direction,
            String tag);

}
