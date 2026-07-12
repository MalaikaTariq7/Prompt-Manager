package com.promptmanager.review_service.dto;

import java.time.LocalDateTime;

public class ReviewResponse {

    private Long id;
    private Long promptId;
    private String reviewerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public ReviewResponse() {
    }

    public ReviewResponse(Long id, Long promptId, String reviewerName, Integer rating, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.promptId = promptId;
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPromptId() {
        return promptId;
    }

    public void setPromptId(Long promptId) {
        this.promptId = promptId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
