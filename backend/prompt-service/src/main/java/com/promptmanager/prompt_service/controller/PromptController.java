package com.promptmanager.prompt_service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import com.promptmanager.prompt_service.dto.PromptRequest;
import com.promptmanager.prompt_service.dto.PromptResponse;
import com.promptmanager.prompt_service.service.PromptService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @PostMapping
    public PromptResponse createPrompt(
            @Valid @RequestBody PromptRequest request) {

        return promptService.createPrompt(request);
    }

    @GetMapping
    public List<PromptResponse> getAllPrompts() {

        return promptService.getAllPrompts();
    }

    @GetMapping("/{id}")
    public PromptResponse getPromptById(
            @PathVariable Long id) {

        return promptService.getPromptById(id);
    }

    @PutMapping("/{id}")
    public PromptResponse updatePrompt(
            @PathVariable Long id,
            @Valid @RequestBody PromptRequest request) {

        return promptService.updatePrompt(id, request);
    }

    @DeleteMapping("/{id}")
    public void deletePrompt(
            @PathVariable Long id) {

        promptService.deletePrompt(id);
    }

    @GetMapping("/search")
    public List<PromptResponse> searchByTitle(
            @RequestParam String title) {

        return promptService.searchByTitle(title);
    }

    @GetMapping("/category")
    public List<PromptResponse> getByCategory(
            @RequestParam String category) {

        return promptService.getPromptsByCategory(category);
    }

    @GetMapping("/page")
    public Page<PromptResponse> getPrompts(

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "5") int size,

            @RequestParam(defaultValue = "createdAt") String sortBy,

            @RequestParam(defaultValue = "desc") String direction) {

        return promptService.getPrompts(
                page,
                size,
                sortBy,
                direction);
    }
}