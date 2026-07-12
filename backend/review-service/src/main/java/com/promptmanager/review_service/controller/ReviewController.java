package com.promptmanager.review_service.controller;

import java.util.List;

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

import com.promptmanager.review_service.dto.ReviewRequest;
import com.promptmanager.review_service.dto.ReviewResponse;
import com.promptmanager.review_service.service.ReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.createReview(request));
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok("Review deleted successfully.");
    }

    @GetMapping("/prompt/{promptId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByPromptId(@PathVariable Long promptId) {
        return ResponseEntity.ok(reviewService.getReviewsByPromptId(promptId));
    }

    @GetMapping("/rating/{rating}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByRating(@PathVariable Integer rating) {
        return ResponseEntity.ok(reviewService.getReviewsByRating(rating));
    }

    @GetMapping("/reviewer")
    public ResponseEntity<List<ReviewResponse>> searchReviewer(@RequestParam String name) {
        return ResponseEntity.ok(reviewService.searchReviewer(name));
    }

    @GetMapping("/page")
    public ResponseEntity<List<ReviewResponse>> getPaginatedReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(reviewService.getPaginatedReviews(page, size, sortBy, direction));
    }
}
