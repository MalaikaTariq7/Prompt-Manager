package com.promptmanager.review_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.promptmanager.review_service.dto.ReviewRequest;
import com.promptmanager.review_service.dto.ReviewResponse;
import com.promptmanager.review_service.entity.Review;
import com.promptmanager.review_service.exception.ReviewNotFoundException;
import com.promptmanager.review_service.mapper.ReviewMapper;
import com.promptmanager.review_service.repository.JsonReviewRepository;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final JsonReviewRepository repository;
    private final PromptServiceClient promptServiceClient;

    public ReviewServiceImpl(JsonReviewRepository repository,
                             PromptServiceClient promptServiceClient) {
        this.repository = repository;
        this.promptServiceClient = promptServiceClient;
    }

    @Override
    public ReviewResponse createReview(ReviewRequest request) {

        // Verify prompt exists in Prompt Service
        promptServiceClient.getPromptById(request.getPromptId());

        Review review = ReviewMapper.toEntity(request);

        Review savedReview = repository.save(review);

        return ReviewMapper.toResponse(savedReview);
    }

    @Override
    public List<ReviewResponse> getAllReviews() {

        return sortReviewsNewestFirst(repository.findAll())
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Override
    public ReviewResponse getReviewById(Long id) {

        Review review = repository.findById(id)
                .orElseThrow(() ->
                        new ReviewNotFoundException(
                                "Review not found with ID: " + id));

        return ReviewMapper.toResponse(review);
    }

    @Override
    public ReviewResponse updateReview(Long id, ReviewRequest request) {

        Review review = repository.findById(id)
                .orElseThrow(() ->
                        new ReviewNotFoundException(
                                "Review not found with ID: " + id));

        // Verify prompt exists before updating
        promptServiceClient.getPromptById(request.getPromptId());

        review.setPromptId(request.getPromptId());
        review.setReviewerName(request.getReviewerName());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review updatedReview = repository.save(review);

        return ReviewMapper.toResponse(updatedReview);
    }

    @Override
    public void deleteReview(Long id) {

        repository.findById(id)
                .orElseThrow(() ->
                        new ReviewNotFoundException(
                                "Review not found with ID: " + id));

        repository.delete(id);
    }

    @Override
    public List<ReviewResponse> getReviewsByPromptId(Long promptId) {

        return sortReviewsNewestFirst(repository.findByPromptId(promptId))
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> getReviewsByRating(Integer rating) {

        return sortReviewsNewestFirst(repository.findByRating(rating))
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> searchReviewer(String reviewerName) {

        return sortReviewsNewestFirst(repository.findByReviewerNameContainingIgnoreCase(reviewerName))
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> getPaginatedReviews(
            int page,
            int size,
            String sortBy,
            String direction) {

        List<Review> reviews = repository.findAll();

        reviews = repository.sort(reviews, sortBy, direction);

        int start = page * size;

        if (start >= reviews.size()) {
            return List.of();
        }

        int end = Math.min(start + size, reviews.size());

        return reviews.subList(start, end)
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    private List<Review> sortReviewsNewestFirst(List<Review> reviews) {
        return reviews.stream()
                .sorted(this::compareReviewsNewestFirst)
                .toList();
    }

    private int compareReviewsNewestFirst(Review first, Review second) {
        LocalDateTime firstCreatedAt = first.getCreatedAt();
        LocalDateTime secondCreatedAt = second.getCreatedAt();

        if (firstCreatedAt == null && secondCreatedAt != null) {
            return 1;
        }
        if (firstCreatedAt != null && secondCreatedAt == null) {
            return -1;
        }
        if (firstCreatedAt != null) {
            int createdAtComparison = secondCreatedAt.compareTo(firstCreatedAt);
            if (createdAtComparison != 0) {
                return createdAtComparison;
            }
        }

        Long firstId = first.getId();
        Long secondId = second.getId();

        if (firstId == null && secondId != null) {
            return 1;
        }
        if (firstId != null && secondId == null) {
            return -1;
        }
        if (firstId == null) {
            return 0;
        }
        return secondId.compareTo(firstId);
    }
}