package com.promptmanager.review_service.service;

import java.util.List;

import com.promptmanager.review_service.dto.ReviewRequest;
import com.promptmanager.review_service.dto.ReviewResponse;

public interface ReviewService {

    ReviewResponse createReview(ReviewRequest request);

    List<ReviewResponse> getAllReviews();

    ReviewResponse getReviewById(Long id);

    ReviewResponse updateReview(Long id, ReviewRequest request);

    void deleteReview(Long id);

    List<ReviewResponse> getReviewsByPromptId(Long promptId);

    List<ReviewResponse> getReviewsByRating(Integer rating);

    List<ReviewResponse> searchReviewer(String reviewerName);

    List<ReviewResponse> getPaginatedReviews(int page, int size, String sortBy, String direction);
}
