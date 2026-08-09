package com.promptmanager.prompt_service.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "prompts")
public class Prompt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String promptText;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String attachmentUrl;

    private String attachmentPublicId;

    public Prompt() {
    }

    public Prompt(UUID id, String title, String description, String promptText,
                  String category, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.promptText = promptText;
        this.category = category;
        this.createdAt = createdAt;
    }

    public Prompt(UUID id, String title, String description, String promptText,
                  String category, LocalDateTime createdAt,
                  String attachmentUrl, String attachmentPublicId) {
        this(id, title, description, promptText, category, createdAt);
        this.attachmentUrl = attachmentUrl;
        this.attachmentPublicId = attachmentPublicId;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
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

    public String getAttachmentPublicId() {
        return attachmentPublicId;
    }

    public void setAttachmentPublicId(String attachmentPublicId) {
        this.attachmentPublicId = attachmentPublicId;
    }

    @Override
    public String toString() {
        return "Prompt{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", promptText='" + promptText + '\'' +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                ", attachmentUrl='" + attachmentUrl + '\'' +
                '}';
    }
}
