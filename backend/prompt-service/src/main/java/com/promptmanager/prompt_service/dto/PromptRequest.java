package com.promptmanager.prompt_service.dto;

import jakarta.validation.constraints.NotBlank;

public class PromptRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Prompt text is required")
    private String promptText;

    @NotBlank(message = "Category is required")
    private String category;

    // Default Constructor
    public PromptRequest() {
    }

    // Parameterized Constructor
    public PromptRequest(String title, String description,
                         String promptText, String category) {
        this.title = title;
        this.description = description;
        this.promptText = promptText;
        this.category = category;
    }

    // Getters & Setters

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
}

