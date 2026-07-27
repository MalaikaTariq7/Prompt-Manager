package com.promptmanager.review_service.repository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.promptmanager.review_service.entity.Review;

import jakarta.annotation.PostConstruct;

@Repository
public class JsonReviewRepository {

    private final ObjectMapper objectMapper;

    @Value("${review.storage.file}")
    private String fileName;

    private File file;

    public JsonReviewRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        file = new File(fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
                objectMapper.writeValue(file, new ArrayList<Review>());
            }
            if (file.length() == 0) {
                objectMapper.writeValue(file, new ArrayList<Review>());
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize reviews.json", e);
        }
    }

    private synchronized List<Review> readAll() {
        try {
            if (file.length() == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, new TypeReference<List<Review>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Unable to read reviews.json", e);
        }
    }

    private synchronized void writeAll(List<Review> reviews) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, reviews);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write reviews.json", e);
        }
    }

    public List<Review> findAll() {
        return readAll();
    }

    public Page<Review> findAll(Pageable pageable, Long promptId) {
        List<Review> reviews = readAll().stream()
                .filter(review -> promptId == null
                        || promptId.equals(review.getPromptId()))
                .toList();

        List<Review> sortedReviews = applySort(reviews, pageable.getSort());

        int totalElements = sortedReviews.size();
        int start = Math.toIntExact(pageable.getOffset());

        if (start >= totalElements) {
            return new PageImpl<>(List.of(), pageable, totalElements);
        }

        int end = Math.min(start + pageable.getPageSize(), totalElements);
        List<Review> content = new ArrayList<>(sortedReviews.subList(start, end));

        return new PageImpl<>(content, pageable, totalElements);
    }

    public Optional<Review> findById(Long id) {
        return readAll().stream()
                .filter(review -> review.getId().equals(id))
                .findFirst();
    }

    public synchronized Review save(Review review) {
        List<Review> reviews = readAll();

        if (review.getId() == null) {
            Long nextId = reviews.stream()
                    .map(Review::getId)
                    .filter(id -> id != null)
                    .max(Long::compareTo)
                    .orElse(0L) + 1;

            review.setId(nextId);
            if (review.getCreatedAt() == null) {
                review.setCreatedAt(LocalDateTime.now());
            }
            reviews.add(review);
        } else {
            boolean updated = false;
            for (int i = 0; i < reviews.size(); i++) {
                if (reviews.get(i).getId().equals(review.getId())) {
                    if (review.getCreatedAt() == null) {
                        review.setCreatedAt(reviews.get(i).getCreatedAt());
                    }
                    reviews.set(i, review);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                reviews.add(review);
            }
        }

        writeAll(reviews);
        return review;
    }

    public synchronized void delete(Long id) {
        List<Review> reviews = readAll();
        reviews.removeIf(review -> review.getId().equals(id));
        writeAll(reviews);
    }

    public List<Review> findByPromptId(Long promptId) {
        return readAll().stream()
                .filter(review -> review.getPromptId().equals(promptId))
                .toList();
    }

    public List<Review> findByRating(Integer rating) {
        return readAll().stream()
                .filter(review -> review.getRating().equals(rating))
                .toList();
    }

    public List<Review> findByReviewerNameContainingIgnoreCase(String reviewerName) {
        String keyword = reviewerName == null ? "" : reviewerName.toLowerCase();
        return readAll().stream()
                .filter(review -> review.getReviewerName() != null
                        && review.getReviewerName().toLowerCase().contains(keyword))
                .toList();
    }

    public List<Review> sort(List<Review> reviews, String sortBy, String direction) {
        List<Review> sortedReviews = new ArrayList<>(reviews);
        Comparator<Review> comparator = comparatorFor(sortBy == null ? "id" : sortBy);

        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        sortedReviews.sort(comparator);
        return sortedReviews;
    }

    private List<Review> applySort(List<Review> reviews, Sort sort) {
        List<Review> sortedReviews = new ArrayList<>(reviews);

        if (sort.isUnsorted()) {
            return sortedReviews;
        }

        Comparator<Review> combinedComparator = null;

        for (Sort.Order order : sort) {
            Comparator<Review> comparator = comparatorFor(order.getProperty());

            if (order.isDescending()) {
                comparator = comparator.reversed();
            }

            combinedComparator = combinedComparator == null
                    ? comparator
                    : combinedComparator.thenComparing(comparator);
        }

        sortedReviews.sort(combinedComparator);
        return sortedReviews;
    }

    private Comparator<Review> comparatorFor(String sortBy) {
        return switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "id" -> Comparator.comparing(Review::getId, Comparator.nullsLast(Long::compareTo));
            case "promptid" -> Comparator.comparing(Review::getPromptId, Comparator.nullsLast(Long::compareTo));
            case "reviewername" -> Comparator.comparing(Review::getReviewerName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "rating" -> Comparator.comparing(Review::getRating, Comparator.nullsLast(Integer::compareTo));
            case "createdat" -> Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
            default -> throw new IllegalArgumentException(
                    "Invalid sortBy field. Allowed values: id, promptId, reviewerName, rating, createdAt");
        };
    }
}