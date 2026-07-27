package com.promptmanager.review_service.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.promptmanager.review_service.entity.Review;

class JsonReviewRepositoryPaginationTest {

    @TempDir
    Path tempDir;

    private JsonReviewRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JsonReviewRepository();
        ReflectionTestUtils.setField(
                repository,
                "fileName",
                tempDir.resolve("reviews.json").toString()
        );
        repository.init();
    }

    @Test
    void returnsDefaultFirstPageSortedByCreatedAtDescending() {
        seedReviews();

        Page<Review> page = repository.findAll(
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                ),
                null
        );

        assertThat(page.getContent())
                .extracting(Review::getId)
                .containsExactly(6L, 5L, 4L, 3L, 2L, 1L);
        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(10);
    }

    @Test
    void returnsSecondPageWithMetadata() {
        seedReviews();

        Page<Review> page = repository.findAll(
                PageRequest.of(
                        1,
                        2,
                        Sort.by(Sort.Direction.ASC, "id")
                ),
                null
        );

        assertThat(page.getContent())
                .extracting(Review::getId)
                .containsExactly(3L, 4L);
        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getNumberOfElements()).isEqualTo(2);
    }

    @Test
    void filtersByPromptIdBeforePaging() {
        seedReviews();

        Page<Review> page = repository.findAll(
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.ASC, "id")
                ),
                20L
        );

        assertThat(page.getContent())
                .extracting(Review::getPromptId)
                .containsOnly(20L);
        assertThat(page.getContent())
                .extracting(Review::getId)
                .containsExactly(2L, 3L, 6L);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void sortsAscendingAndDescending() {
        seedReviews();

        Page<Review> ascending = repository.findAll(
                PageRequest.of(
                        0,
                        3,
                        Sort.by(Sort.Direction.ASC, "rating")
                ),
                null
        );

        Page<Review> descending = repository.findAll(
                PageRequest.of(
                        0,
                        3,
                        Sort.by(Sort.Direction.DESC, "rating")
                ),
                null
        );

        assertThat(ascending.getContent())
                .extracting(Review::getRating)
                .containsExactly(1, 2, 3);
        assertThat(descending.getContent())
                .extracting(Review::getRating)
                .containsExactly(5, 4, 3);
    }

    @Test
    void rejectsInvalidSortBy() {
        seedReviews();

        assertThatThrownBy(() -> repository.findAll(
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.ASC, "badField")
                ),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sortBy field");
    }

    @Test
    void returnsEmptyPageWhenFilterMatchesNothing() {
        seedReviews();

        Page<Review> page = repository.findAll(
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                ),
                999L
        );

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getTotalPages()).isZero();
        assertThat(page.isEmpty()).isTrue();
    }

    private void seedReviews() {
        repository.save(review(1L, 10L, "Ava", 1, 1));
        repository.save(review(2L, 20L, "Ben", 2, 2));
        repository.save(review(3L, 20L, "Cara", 3, 3));
        repository.save(review(4L, 30L, "Drew", 4, 4));
        repository.save(review(5L, 30L, "Eli", 5, 5));
        repository.save(review(6L, 20L, "Malaika", 3, 6));
    }

    private Review review(
            Long id,
            Long promptId,
            String reviewerName,
            Integer rating,
            int createdAtDay) {

        return new Review(
                id,
                promptId,
                reviewerName,
                rating,
                "Comment " + id,
                LocalDateTime.of(2026, 7, createdAtDay, 12, 0)
        );
    }
}