package com.promptmanager.review_service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.promptmanager.review_service.dto.ReviewDigestResponse;
import com.promptmanager.review_service.dto.ReviewRequest;
import com.promptmanager.review_service.dto.ReviewResponse;
import com.promptmanager.review_service.service.ReviewDigestService;
import com.promptmanager.review_service.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewDigestService
            reviewDigestService;

    public ReviewController(
            ReviewService reviewService,
            ReviewDigestService reviewDigestService) {

        this.reviewService = reviewService;
        this.reviewDigestService =
                reviewDigestService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse>
            createReview(
                    @Valid
                    @RequestBody
                    ReviewRequest request) {

        return ResponseEntity.ok(
                reviewService.createReview(request)
        );
    }

    @Operation(summary = "Get paginated reviews")
    @GetMapping
    public ResponseEntity<Page<ReviewResponse>>
            getReviews(
                    @Parameter(description = "Zero-based page number")
                    @RequestParam(defaultValue = "0")
                    int page,

                    @Parameter(description = "Page size")
                    @RequestParam(defaultValue = "10")
                    int size,

                    @Parameter(description = "Sort field: id, promptId, reviewerName, rating, createdAt")
                    @RequestParam(defaultValue = "createdAt")
                    String sortBy,

                    @Parameter(description = "Sort direction: asc or desc")
                    @RequestParam(defaultValue = "desc")
                    String direction,

                    @Parameter(description = "Optional prompt id filter")
                    @RequestParam(required = false)
                    String promptId) {

        return ResponseEntity.ok(
                reviewService.getReviews(
                        page,
                        size,
                        sortBy,
                        direction,
                        promptId
                )
        );
    }

    @GetMapping("/digest/latest")
    public ResponseEntity<ReviewDigestResponse>
            getLatestDigest() {

        return ResponseEntity.ok(
                reviewDigestService.getLatestDigest()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse>
            getReviewById(
                    @PathVariable Long id) {

        return ResponseEntity.ok(
                reviewService.getReviewById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse>
            updateReview(
                    @PathVariable Long id,
                    @Valid
                    @RequestBody
                    ReviewRequest request) {

        return ResponseEntity.ok(
                reviewService.updateReview(
                        id,
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long id) {

        reviewService.deleteReview(id);

        return ResponseEntity.ok(
                "Review deleted successfully."
        );
    }

    @GetMapping("/prompt/{promptId}")
    public ResponseEntity<List<ReviewResponse>>
            getReviewsByPromptId(
                    @PathVariable String promptId) {

        return ResponseEntity.ok(
                reviewService
                        .getReviewsByPromptId(
                                promptId
                        )
        );
    }

    @GetMapping("/rating/{rating}")
    public ResponseEntity<List<ReviewResponse>>
            getReviewsByRating(
                    @PathVariable Integer rating) {

        return ResponseEntity.ok(
                reviewService
                        .getReviewsByRating(rating)
        );
    }

    @GetMapping("/reviewer")
    public ResponseEntity<List<ReviewResponse>>
            searchReviewer(
                    @RequestParam String name) {

        return ResponseEntity.ok(
                reviewService.searchReviewer(name)
        );
    }
}