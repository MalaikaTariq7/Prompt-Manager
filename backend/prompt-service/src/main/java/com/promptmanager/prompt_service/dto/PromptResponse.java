package com.promptmanager.prompt_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class PromptResponse {

    private UUID id;

    private String title;

    private String description;

    private String promptText;

    private String category;

    private LocalDateTime createdAt;

    private String attachmentUrl;

    public PromptResponse() {
    }

    public PromptResponse(UUID id,
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

    public PromptResponse(UUID id,
                          String title,
                          String description,
                          String promptText,
                          String category,
                          LocalDateTime createdAt,
                          String attachmentUrl) {

        this(id, title, description, promptText, category, createdAt);
        this.attachmentUrl = attachmentUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }
}
