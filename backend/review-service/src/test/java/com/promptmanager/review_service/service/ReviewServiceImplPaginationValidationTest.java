package com.promptmanager.review_service.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.promptmanager.review_service.repository.JsonReviewRepository;

class ReviewServiceImplPaginationValidationTest {

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                mock(JsonReviewRepository.class),
                mock(PromptServiceClient.class),
                mock(ReviewNotificationService.class)
        );
    }

    @Test
    void rejectsInvalidPage() {
        assertThatThrownBy(() -> reviewService.getReviews(
                -1,
                10,
                "createdAt",
                "desc",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page must be greater than or equal to 0");
    }

    @Test
    void rejectsInvalidSize() {
        assertThatThrownBy(() -> reviewService.getReviews(
                0,
                0,
                "createdAt",
                "desc",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be greater than 0");
    }

    @Test
    void rejectsInvalidDirection() {
        assertThatThrownBy(() -> reviewService.getReviews(
                0,
                10,
                "createdAt",
                "sideways",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("direction must be either asc or desc");
    }

    @Test
    void rejectsInvalidSortBy() {
        assertThatThrownBy(() -> reviewService.getReviews(
                0,
                10,
                "comment",
                "desc",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sortBy must be one of");
    }
}