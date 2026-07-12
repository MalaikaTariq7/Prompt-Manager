package com.promptmanager.prompt_service.mapper;

import com.promptmanager.prompt_service.dto.PromptRequest;
import com.promptmanager.prompt_service.dto.PromptResponse;
import com.promptmanager.prompt_service.entity.Prompt;

public class PromptMapper {

    public static Prompt toEntity(PromptRequest request) {
        Prompt prompt = new Prompt();

        prompt.setTitle(request.getTitle());
        prompt.setDescription(request.getDescription());
        prompt.setPromptText(request.getPromptText());
        prompt.setCategory(request.getCategory());

        return prompt;
    }

    public static PromptResponse toResponse(Prompt prompt) {
        return new PromptResponse(
                prompt.getId(),
                prompt.getTitle(),
                prompt.getDescription(),
                prompt.getPromptText(),
                prompt.getCategory(),
                prompt.getCreatedAt()
        );
    }
}