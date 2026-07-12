package com.promptmanager.prompt_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.promptmanager.prompt_service.dto.PromptRequest;
import com.promptmanager.prompt_service.dto.PromptResponse;
import com.promptmanager.prompt_service.entity.Prompt;
import com.promptmanager.prompt_service.exception.PromptNotFoundException;
import com.promptmanager.prompt_service.mapper.PromptMapper;
import com.promptmanager.prompt_service.repository.PromptRepository;

@Service
public class PromptServiceImpl implements PromptService {

    private final PromptRepository promptRepository;

    public PromptServiceImpl(PromptRepository promptRepository) {
        this.promptRepository = promptRepository;
    }

    @Override
    public PromptResponse createPrompt(PromptRequest request) {

        Prompt prompt = PromptMapper.toEntity(request);

        prompt.setCreatedAt(LocalDateTime.now());

        Prompt savedPrompt = promptRepository.save(prompt);

        return PromptMapper.toResponse(savedPrompt);
    }

    @Override
    public List<PromptResponse> getAllPrompts() {

        return promptRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .map(PromptMapper::toResponse)
                .toList();
    }

    @Override
    public PromptResponse getPromptById(Long id) {

        Prompt prompt = promptRepository.findById(id)
                .orElseThrow(() ->
                        new PromptNotFoundException("Prompt not found with id : " + id));

        return PromptMapper.toResponse(prompt);
    }

    @Override
    public PromptResponse updatePrompt(Long id, PromptRequest request) {

        Prompt prompt = promptRepository.findById(id)
                .orElseThrow(() ->
                        new PromptNotFoundException("Prompt not found with id : " + id));

        prompt.setTitle(request.getTitle());
        prompt.setDescription(request.getDescription());
        prompt.setPromptText(request.getPromptText());
        prompt.setCategory(request.getCategory());

        Prompt updatedPrompt = promptRepository.save(prompt);

        return PromptMapper.toResponse(updatedPrompt);
    }

    @Override
    public void deletePrompt(Long id) {

        Prompt prompt = promptRepository.findById(id)
                .orElseThrow(() ->
                        new PromptNotFoundException("Prompt not found with id : " + id));

        promptRepository.delete(prompt);
    }

    @Override
    public List<PromptResponse> searchByTitle(String title) {

        return sortPromptsNewestFirst(promptRepository.findByTitleContainingIgnoreCase(title))
                .stream()
                .map(PromptMapper::toResponse)
                .toList();
    }

    @Override
    public List<PromptResponse> getPromptsByCategory(String category) {

        return sortPromptsNewestFirst(promptRepository.findByCategoryIgnoreCase(category))
                .stream()
                .map(PromptMapper::toResponse)
                .toList();
    }

    @Override
    public Page<PromptResponse> getPrompts(
            int page,
            int size,
            String sortBy,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        PageRequest pageRequest = PageRequest.of(page, size, sort);

        return promptRepository.findAll(pageRequest)
                .map(PromptMapper::toResponse);
    }

    private List<Prompt> sortPromptsNewestFirst(List<Prompt> prompts) {
        return prompts.stream()
                .sorted(this::comparePromptsNewestFirst)
                .toList();
    }

    private int comparePromptsNewestFirst(Prompt first, Prompt second) {
        LocalDateTime firstCreatedAt = first.getCreatedAt();
        LocalDateTime secondCreatedAt = second.getCreatedAt();

        if (firstCreatedAt == null && secondCreatedAt != null) {
            return 1;
        }
        if (firstCreatedAt != null && secondCreatedAt == null) {
            return -1;
        }
        if (firstCreatedAt != null) {
            int createdAtComparison = secondCreatedAt.compareTo(firstCreatedAt);
            if (createdAtComparison != 0) {
                return createdAtComparison;
            }
        }

        Long firstId = first.getId();
        Long secondId = second.getId();

        if (firstId == null && secondId != null) {
            return 1;
        }
        if (firstId != null && secondId == null) {
            return -1;
        }
        if (firstId == null) {
            return 0;
        }
        return secondId.compareTo(firstId);
    }

}