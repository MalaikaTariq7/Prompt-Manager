package com.promptmanager.review_service.dto;

import java.time.LocalDateTime;

public class ReviewDigestResponse {

    private long totalReviews;
    private double averageScore;
    private String highestScoringPromptId;
    private LocalDateTime generatedAt;

    public ReviewDigestResponse() {
    }

    public ReviewDigestResponse(
            long totalReviews,
            double averageScore,
            String highestScoringPromptId,
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

    public String getHighestScoringPromptId() {
        return highestScoringPromptId;
    }

    public void setHighestScoringPromptId(
            String highestScoringPromptId) {

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