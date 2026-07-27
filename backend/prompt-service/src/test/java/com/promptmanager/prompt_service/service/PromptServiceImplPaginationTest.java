package com.promptmanager.prompt_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.promptmanager.prompt_service.dto.PromptResponse;
import com.promptmanager.prompt_service.entity.Prompt;
import com.promptmanager.prompt_service.repository.PromptRepository;

class PromptServiceImplPaginationTest {

    private PromptRepository promptRepository;
    private PromptServiceImpl promptService;

    @BeforeEach
    void setUp() {
        promptRepository = mock(PromptRepository.class);
        promptService = new PromptServiceImpl(
                promptRepository,
                mock(CloudinaryService.class)
        );
    }

    @Test
    void getsDefaultPaginatedPrompts() {
        Prompt prompt = prompt(1L, "writing");
        when(promptRepository.findAll(org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(prompt)));

        Page<PromptResponse> response = promptService.getPrompts(
                0,
                10,
                "createdAt",
                "desc",
                null
        );

        assertThat(response.getContent())
                .extracting(PromptResponse::getId)
                .containsExactly(1L);
        verify(promptRepository)
                .findAll(org.mockito.ArgumentMatchers.<Pageable>argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 10
                                && pageable.getSort().getOrderFor("createdAt") != null
                                && pageable.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC
                ));
    }

    @Test
    void filtersByTagUsingCategoryField() {
        Prompt prompt = prompt(2L, "coding");
        when(promptRepository.findByCategoryIgnoreCase(
                eq("coding"),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        ))
                .thenReturn(new PageImpl<>(List.of(prompt)));

        Page<PromptResponse> response = promptService.getPrompts(
                0,
                10,
                "createdAt",
                "asc",
                "coding"
        );

        assertThat(response.getContent())
                .extracting(PromptResponse::getCategory)
                .containsExactly("coding");
        verify(promptRepository)
                .findByCategoryIgnoreCase(
                        eq("coding"),
                        org.mockito.ArgumentMatchers.argThat(pageable ->
                                pageable.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.ASC
                        )
                );
    }

    @Test
    void rejectsInvalidPage() {
        assertThatThrownBy(() -> promptService.getPrompts(
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
        assertThatThrownBy(() -> promptService.getPrompts(
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
        assertThatThrownBy(() -> promptService.getPrompts(
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
        assertThatThrownBy(() -> promptService.getPrompts(
                0,
                10,
                "badField",
                "desc",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sortBy must be one of");
    }

    private Prompt prompt(Long id, String category) {
        return new Prompt(
                id,
                "Title " + id,
                "Description " + id,
                "Prompt text " + id,
                category,
                LocalDateTime.of(2026, 7, 27, 12, 0)
        );
    }
}