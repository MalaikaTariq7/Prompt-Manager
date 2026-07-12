package com.promptmanager.review_service.mapper;

import com.promptmanager.review_service.dto.ReviewRequest;
import com.promptmanager.review_service.dto.ReviewResponse;
import com.promptmanager.review_service.entity.Review;

public class ReviewMapper {

    public static Review toEntity(ReviewRequest request) {
        Review review = new Review();
        review.setPromptId(request.getPromptId());
        review.setReviewerName(request.getReviewerName());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        return review;
    }

    public static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getPromptId(),
                review.getReviewerName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
