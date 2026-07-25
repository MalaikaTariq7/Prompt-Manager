package com.promptmanager.review_service.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.promptmanager.review_service.dto.ReviewDigestResponse;
import com.promptmanager.review_service.entity.Review;
import com.promptmanager.review_service.repository.JsonReviewRepository;

@Service
public class ReviewDigestService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ReviewDigestService.class
            );

    private final JsonReviewRepository repository;

    private final AtomicReference<ReviewDigestResponse>
            latestDigest = new AtomicReference<>();

    public ReviewDigestService(
            JsonReviewRepository repository) {

        this.repository = repository;
    }

    @Scheduled(
            fixedDelayString = "${digest.interval-ms}",
            initialDelayString = "${digest.initial-delay-ms:5000}"
    )
    public void generateDigest() {

        List<Review> reviews = repository.findAll();

        long totalReviews = reviews.size();

        double averageScore = reviews.stream()
                .map(Review::getRating)
                .filter(rating -> rating != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        Long highestScoringPromptId =
                calculateHighestScoringPrompt(reviews);

        ReviewDigestResponse digest =
                new ReviewDigestResponse(
                        totalReviews,
                        averageScore,
                        highestScoringPromptId,
                        LocalDateTime.now()
                );

        latestDigest.set(digest);

        LOGGER.info(
                "Review digest generated: totalReviews={}, "
                        + "averageScore={}, "
                        + "highestScoringPromptId={}",
                totalReviews,
                averageScore,
                highestScoringPromptId
        );
    }

    public ReviewDigestResponse getLatestDigest() {

        ReviewDigestResponse digest =
                latestDigest.get();

        if (digest == null) {
            generateDigest();
            digest = latestDigest.get();
        }

        return digest;
    }

    private Long calculateHighestScoringPrompt(
            List<Review> reviews) {

        Map<Long, Double> averageScoresByPrompt =
                reviews.stream()
                        .filter(review ->
                                review.getPromptId() != null
                                        && review.getRating() != null
                        )
                        .collect(
                                Collectors.groupingBy(
                                        Review::getPromptId,
                                        Collectors.averagingInt(
                                                Review::getRating
                                        )
                                )
                        );

        return averageScoresByPrompt.entrySet()
                .stream()
                .max(
                        Comparator
                                .<Map.Entry<Long, Double>>
                                        comparingDouble(
                                                Map.Entry::getValue
                                        )
                                .thenComparing(
                                        Map.Entry::getKey,
                                        Comparator.reverseOrder()
                                )
                )
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}