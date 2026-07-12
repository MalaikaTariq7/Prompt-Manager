package com.promptmanager.review_service.dto;

import java.time.LocalDateTime;

public class PromptResponse {

    private Long id;

    private String title;

    private String description;

    private String promptText;

    private String category;

    private LocalDateTime createdAt;

    public PromptResponse() {
    }

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

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPromptText() {
        return promptText;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}