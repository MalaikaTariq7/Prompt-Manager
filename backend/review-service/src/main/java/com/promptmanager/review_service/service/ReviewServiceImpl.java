package com.promptmanager.review_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.promptmanager.review_service.dto.ReviewRequest;
import com.promptmanager.review_service.dto.ReviewResponse;
import com.promptmanager.review_service.entity.Review;
import com.promptmanager.review_service.exception.ReviewNotFoundException;
import com.promptmanager.review_service.mapper.ReviewMapper;
import com.promptmanager.review_service.repository.JsonReviewRepository;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Set<String> VALID_SORT_FIELDS = Set.of(
            "id",
            "promptId",
            "reviewerName",
            "rating",
            "createdAt"
    );

    private final JsonReviewRepository repository;
    private final PromptServiceClient promptServiceClient;
    private final ReviewNotificationService
            reviewNotificationService;

    public ReviewServiceImpl(
            JsonReviewRepository repository,
            PromptServiceClient promptServiceClient,
            ReviewNotificationService
                    reviewNotificationService) {

        this.repository = repository;
        this.promptServiceClient =
                promptServiceClient;
        this.reviewNotificationService =
                reviewNotificationService;
    }

    @Override
    public ReviewResponse createReview(
            ReviewRequest request) {

        promptServiceClient.getPromptById(
                request.getPromptId()
        );

        Review review =
                ReviewMapper.toEntity(request);

        Review savedReview =
                repository.save(review);

        reviewNotificationService
                .sendReviewNotification(
                        savedReview
                );

        return ReviewMapper.toResponse(
                savedReview
        );
    }

    @Override
    public List<ReviewResponse> getAllReviews() {

        return sortReviewsNewestFirst(
                repository.findAll()
        )
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Override
    public Page<ReviewResponse> getReviews(
            int page,
            int size,
            String sortBy,
            String direction,
            Long promptId) {

        validatePaginationParameters(
                page,
                size,
                sortBy,
                direction
        );

        Sort.Direction sortDirection =
                Sort.Direction.fromString(direction);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        return repository.findAll(pageRequest, promptId)
                .map(ReviewMapper::toResponse);
    }

    @Override
    public ReviewResponse getReviewById(Long id) {

        Review review = repository.findById(id)
                .orElseThrow(() ->
                        new ReviewNotFoundException(
                                "Review not found with ID: "
                                        + id
                        )
                );

        return ReviewMapper.toResponse(review);
    }

    @Override
    public ReviewResponse updateReview(
            Long id,
            ReviewRequest request) {

        Review review = repository.findById(id)
                .orElseThrow(() ->
                        new ReviewNotFoundException(
                                "Review not found with ID: "
                                        + id
                        )
                );

        promptServiceClient.getPromptById(
                request.getPromptId()
        );

        review.setPromptId(
                request.getPromptId()
        );

        review.setReviewerName(
                request.getReviewerName()
        );

        review.setRating(
                request.getRating()
        );

        review.setComment(
                request.getComment()
        );

        Review updatedReview =
                repository.save(review);

        return ReviewMapper.toResponse(
                updatedReview
        );
    }

    @Override
    public void deleteReview(Long id) {

        repository.findById(id)
                .orElseThrow(() ->
                        new ReviewNotFoundException(
                                "Review not found with ID: "
                                        + id
                        )
                );

        repository.delete(id);
    }

    @Override
    public List<ReviewResponse>
            getReviewsByPromptId(Long promptId) {

        return sortReviewsNewestFirst(
                repository.findByPromptId(promptId)
        )
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse>
            getReviewsByRating(Integer rating) {

        return sortReviewsNewestFirst(
                repository.findByRating(rating)
        )
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> searchReviewer(
            String reviewerName) {

        return sortReviewsNewestFirst(
                repository
                        .findByReviewerNameContainingIgnoreCase(
                                reviewerName
                        )
        )
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
    }

    private void validatePaginationParameters(
            int page,
            int size,
            String sortBy,
            String direction) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be greater than or equal to 0");
        }

        if (size <= 0) {
            throw new IllegalArgumentException(
                    "size must be greater than 0");
        }

        if (direction == null
                || !("asc".equalsIgnoreCase(direction)
                || "desc".equalsIgnoreCase(direction))) {

            throw new IllegalArgumentException(
                    "direction must be either asc or desc");
        }

        if (sortBy == null
                || !VALID_SORT_FIELDS.contains(sortBy)) {

            throw new IllegalArgumentException(
                    "sortBy must be one of: id, promptId, reviewerName, rating, createdAt");
        }
    }

    private List<Review> sortReviewsNewestFirst(
            List<Review> reviews) {

        return reviews.stream()
                .sorted(
                        this::compareReviewsNewestFirst
                )
                .toList();
    }

    private int compareReviewsNewestFirst(
            Review first,
            Review second) {

        LocalDateTime firstCreatedAt =
                first.getCreatedAt();

        LocalDateTime secondCreatedAt =
                second.getCreatedAt();

        if (firstCreatedAt == null
                && secondCreatedAt != null) {

            return 1;
        }

        if (firstCreatedAt != null
                && secondCreatedAt == null) {

            return -1;
        }

        if (firstCreatedAt != null) {

            int createdAtComparison =
                    secondCreatedAt.compareTo(
                            firstCreatedAt
                    );

            if (createdAtComparison != 0) {
                return createdAtComparison;
            }
        }

        Long firstId = first.getId();
        Long secondId = second.getId();

        if (firstId == null
                && secondId != null) {

            return 1;
        }

        if (firstId != null
                && secondId == null) {

            return -1;
        }

        if (firstId == null) {
            return 0;
        }

        return secondId.compareTo(firstId);
    }
}