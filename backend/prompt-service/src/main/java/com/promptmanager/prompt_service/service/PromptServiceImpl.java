package com.promptmanager.prompt_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.promptmanager.prompt_service.dto.PromptRequest;
import com.promptmanager.prompt_service.dto.PromptResponse;
import com.promptmanager.prompt_service.entity.Prompt;
import com.promptmanager.prompt_service.exception.PromptNotFoundException;
import com.promptmanager.prompt_service.mapper.PromptMapper;
import com.promptmanager.prompt_service.repository.PromptRepository;

@Service
public class PromptServiceImpl implements PromptService {

    private static final String PROMPT_CACHE = "prompts";

    private static final Set<String> VALID_SORT_FIELDS = Set.of(
            "id",
            "title",
            "description",
            "promptText",
            "category",
            "createdAt",
            "attachmentUrl",
            "attachmentPublicId"
    );

    private final PromptRepository promptRepository;
    private final CloudinaryService cloudinaryService;

    public PromptServiceImpl(
            PromptRepository promptRepository,
            CloudinaryService cloudinaryService) {

        this.promptRepository = promptRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public PromptResponse createPrompt(PromptRequest request) {

        Prompt prompt = PromptMapper.toEntity(request);

        prompt.setCreatedAt(LocalDateTime.now());

        Prompt savedPrompt = promptRepository.save(prompt);

        return PromptMapper.toResponse(savedPrompt);
    }

    @Override
    public List<PromptResponse> getAllPrompts() {

        return promptRepository
                .findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt")
                                .and(Sort.by(Sort.Direction.DESC, "id"))
                )
                .stream()
                .map(PromptMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = PROMPT_CACHE, key = "#id")
    public PromptResponse getPromptById(Long id) {

        System.out.println(
                "DATABASE HIT: Loading prompt with id " + id
        );

        Prompt prompt = promptRepository.findById(id)
                .orElseThrow(() ->
                        new PromptNotFoundException(
                                "Prompt not found with id : " + id
                        )
                );

        return PromptMapper.toResponse(prompt);
    }

    @Override
    @CachePut(value = PROMPT_CACHE, key = "#id")
    public PromptResponse updatePrompt(
            Long id,
            PromptRequest request) {

        Prompt prompt = promptRepository.findById(id)
                .orElseThrow(() ->
                        new PromptNotFoundException(
                                "Prompt not found with id : " + id
                        )
                );

        prompt.setTitle(request.getTitle());
        prompt.setDescription(request.getDescription());
        prompt.setPromptText(request.getPromptText());
        prompt.setCategory(request.getCategory());

        Prompt updatedPrompt = promptRepository.save(prompt);

        return PromptMapper.toResponse(updatedPrompt);
    }

    @Override
    @CacheEvict(value = PROMPT_CACHE, key = "#id")
    public void deletePrompt(Long id) {

        Prompt prompt = promptRepository.findById(id)
                .orElseThrow(() ->
                        new PromptNotFoundException(
                                "Prompt not found with id : " + id
                        )
                );

        if (prompt.getAttachmentPublicId() != null
                && !prompt.getAttachmentPublicId().isBlank()) {

            cloudinaryService.delete(
                    prompt.getAttachmentPublicId()
            );
        }

        promptRepository.delete(prompt);
    }

    @Override
    @CachePut(value = PROMPT_CACHE, key = "#promptId")
    public PromptResponse uploadAttachment(
            Long promptId,
            MultipartFile file) {

        Prompt prompt = findPromptById(promptId);

        if (prompt.getAttachmentPublicId() != null
                && !prompt.getAttachmentPublicId().isBlank()) {

            cloudinaryService.delete(
                    prompt.getAttachmentPublicId()
            );
        }

        CloudinaryService.UploadResult uploadResult =
                cloudinaryService.upload(file);

        prompt.setAttachmentUrl(
                uploadResult.secureUrl()
        );

        prompt.setAttachmentPublicId(
                uploadResult.publicId()
        );

        Prompt updatedPrompt =
                promptRepository.save(prompt);

        return PromptMapper.toResponse(updatedPrompt);
    }

    @Override
    @CachePut(value = PROMPT_CACHE, key = "#promptId")
    public PromptResponse deleteAttachment(Long promptId) {

        Prompt prompt = findPromptById(promptId);

        if (prompt.getAttachmentPublicId() == null
                || prompt.getAttachmentPublicId().isBlank()) {

            throw new PromptNotFoundException(
                    "Prompt with id " + promptId
                            + " has no attachment to remove"
            );
        }

        cloudinaryService.delete(
                prompt.getAttachmentPublicId()
        );

        prompt.setAttachmentUrl(null);
        prompt.setAttachmentPublicId(null);

        Prompt updatedPrompt =
                promptRepository.save(prompt);

        return PromptMapper.toResponse(updatedPrompt);
    }

    @Override
    public List<PromptResponse> searchByTitle(String title) {

        return sortPromptsNewestFirst(
                promptRepository
                        .findByTitleContainingIgnoreCase(title)
        )
                .stream()
                .map(PromptMapper::toResponse)
                .toList();
    }

    @Override
    public List<PromptResponse> getPromptsByCategory(
            String category) {

        return sortPromptsNewestFirst(
                promptRepository
                        .findByCategoryIgnoreCase(category)
        )
                .stream()
                .map(PromptMapper::toResponse)
                .toList();
    }

    @Override
    public Page<PromptResponse> getPrompts(
            int page,
            int size,
            String sortBy,
            String direction,
            String tag) {

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

        Page<Prompt> prompts = tag == null || tag.isBlank()
                ? promptRepository.findAll(pageRequest)
                : promptRepository.findByCategoryIgnoreCase(
                        tag,
                        pageRequest
                );

        return prompts.map(PromptMapper::toResponse);
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
                    "sortBy must be one of: id, title, description, promptText, category, createdAt, attachmentUrl, attachmentPublicId");
        }
    }

    private Prompt findPromptById(Long id) {

        return promptRepository.findById(id)
                .orElseThrow(() ->
                        new PromptNotFoundException(
                                "Prompt not found with id : " + id
                        )
                );
    }

    private List<Prompt> sortPromptsNewestFirst(
            List<Prompt> prompts) {

        return prompts.stream()
                .sorted(this::comparePromptsNewestFirst)
                .toList();
    }

    private int comparePromptsNewestFirst(
            Prompt first,
            Prompt second) {

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