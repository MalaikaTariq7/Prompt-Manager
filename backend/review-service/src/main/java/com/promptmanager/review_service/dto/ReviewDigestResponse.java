package com.promptmanager.review_service.dto;

import java.time.LocalDateTime;

public class ReviewDigestResponse {

    private long totalReviews;
    private double averageScore;
    private Long highestScoringPromptId;
    private LocalDateTime generatedAt;

    public ReviewDigestResponse() {
    }

    public ReviewDigestResponse(
            long totalReviews,
            double averageScore,
            Long highestScoringPromptId,
            LocalDateTime generatedAt) {

        this.totalReviews = totalReviews;
        this.averageScore = averageScore;
        this.highestScoringPromptId = highestScoringPromptId;
        this.generatedAt = generatedAt;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public Long getHighestScoringPromptId() {
        return highestScoringPromptId;
    }

    public void setHighestScoringPromptId(
            Long highestScoringPromptId) {

        this.highestScoringPromptId =
                highestScoringPromptId;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(
            LocalDateTime generatedAt) {

        this.generatedAt = generatedAt;
    }
}