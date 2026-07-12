package com.promptmanager.prompt_service.dto;

import java.time.LocalDateTime;

public class PromptResponse {

    private Long id;

    private String title;

    private String description;

    private String promptText;

    private String category;

    private LocalDateTime createdAt;

    // Default Constructor
    public PromptResponse() {
    }

    // Parameterized Constructor
    public PromptResponse(Long id,
                          String title,
                          String description,
                          String promptText,
                          String category,
                          LocalDateTime createdAt) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.promptText = promptText;
        this.category = category;
        this.createdAt = createdAt;
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}