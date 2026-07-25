package com.promptmanager.review_service.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.promptmanager.review_service.entity.Review;

@Service
public class ReviewNotificationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ReviewNotificationService.class
            );

    private final Path notificationLogPath;
    private final long simulatedDelayMs;

    public ReviewNotificationService(
            @Value("${notification.log.file}")
            String notificationLogFile,

            @Value("${notification.delay-ms:3000}")
            long simulatedDelayMs) {

        this.notificationLogPath =
                Path.of(notificationLogFile);

        this.simulatedDelayMs =
                simulatedDelayMs;
    }

    @Async("reviewTaskExecutor")
    public void sendReviewNotification(
            Review review) {

        try {
            LOGGER.info(
                    "Async notification started for review {}",
                    review.getId()
            );

            Thread.sleep(simulatedDelayMs);

            String notification =
                    LocalDateTime.now()
                            + " | reviewer="
                            + review.getReviewerName()
                            + " | promptId="
                            + review.getPromptId()
                            + " | rating="
                            + review.getRating()
                            + System.lineSeparator();

            Files.writeString(
                    notificationLogPath,
                    notification,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            LOGGER.info(
                    "Async notification completed for review {}",
                    review.getId()
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            LOGGER.error(
                    "Async notification interrupted for review {}",
                    review.getId(),
                    exception
            );

        } catch (IOException exception) {

            LOGGER.error(
                    "Could not write notification for review {}",
                    review.getId(),
                    exception
            );
        }
    }
}