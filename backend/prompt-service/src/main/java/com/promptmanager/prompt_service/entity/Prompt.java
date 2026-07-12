package com.promptmanager.prompt_service.entity;

import java.time.LocalDateTime;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // Default Constructor
    public Prompt() {
    }

    // Parameterized Constructor
    public Prompt(Long id, String title, String description, String promptText,
                  String category, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.promptText = promptText;
        this.category = category;
        this.createdAt = createdAt;
    }

    // Automatically sets createdAt before saving to database
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters

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

    // Setter is optional because @PrePersist sets it automatically,
    // but we keep it for flexibility.
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
                '}';
    }
}