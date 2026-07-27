package com.promptmanager.review_service.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.promptmanager.review_service.dto.ReviewRequest;
import com.promptmanager.review_service.dto.ReviewResponse;

public interface ReviewService {

    ReviewResponse createReview(
            ReviewRequest request);

    List<ReviewResponse> getAllReviews();

    Page<ReviewResponse> getReviews(
            int page,
            int size,
            String sortBy,
            String direction,
            Long promptId);

    ReviewResponse getReviewById(Long id);

    ReviewResponse updateReview(
            Long id,
            ReviewRequest request);

    void deleteReview(Long id);

    List<ReviewResponse> getReviewsByPromptId(
            Long promptId);

    List<ReviewResponse> getReviewsByRating(
            Integer rating);

    List<ReviewResponse> searchReviewer(
            String reviewerName);
}