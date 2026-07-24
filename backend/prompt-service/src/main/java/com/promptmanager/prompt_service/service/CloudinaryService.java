package com.promptmanager.prompt_service.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.promptmanager.prompt_service.exception.AttachmentStorageException;

@Service
public class CloudinaryService {

    private static final String ATTACHMENT_FOLDER = "prompt-manager/attachments";
    private static final String RESOURCE_TYPE = "raw";

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryService(
            Cloudinary cloudinary,
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        this.cloudinary = cloudinary;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public UploadResult upload(MultipartFile file) {
        validateFile(file);
        validateCloudinaryCredentials();

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    Map.of(
                            "folder", ATTACHMENT_FOLDER,
                            "resource_type", RESOURCE_TYPE,
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");
            Object publicId = uploadResult.get("public_id");

            if (secureUrl == null || publicId == null) {
                throw new AttachmentStorageException(
                        "Cloudinary upload did not return attachment details");
            }

            return new UploadResult(
                    secureUrl.toString(),
                    publicId.toString()
            );
        } catch (IOException ex) {
            throw new AttachmentStorageException(
                    "Failed to read attachment before upload", ex);
        } catch (RuntimeException ex) {
            if (ex instanceof AttachmentStorageException) {
                throw ex;
            }
            throw new AttachmentStorageException(
                    "Failed to upload attachment to Cloudinary", ex);
        }
    }

    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        validateCloudinaryCredentials();

        try {
            Map<?, ?> deleteResult = cloudinary.uploader().destroy(
                    publicId,
                    Map.of("resource_type", RESOURCE_TYPE)
            );

            Object result = deleteResult.get("result");
            if (result != null
                    && !"ok".equals(result.toString())
                    && !"not found".equals(result.toString())) {
                throw new AttachmentStorageException(
                        "Cloudinary delete failed with result: " + result);
            }
        } catch (IOException ex) {
            throw new AttachmentStorageException(
                    "Failed to delete attachment from Cloudinary", ex);
        } catch (RuntimeException ex) {
            if (ex instanceof AttachmentStorageException) {
                throw ex;
            }
            throw new AttachmentStorageException(
                    "Failed to delete attachment from Cloudinary", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Attachment file must not be empty");
        }
    }

    private void validateCloudinaryCredentials() {
        if (isDummyOrBlank(cloudName)
                || isDummyOrBlank(apiKey)
                || isDummyOrBlank(apiSecret)) {
            throw new AttachmentStorageException(
                    "Cloudinary credentials are not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET before uploading attachments.");
        }
    }

    private boolean isDummyOrBlank(String value) {
        return value == null
                || value.isBlank()
                || value.startsWith("dummy-");
    }

    public record UploadResult(String secureUrl, String publicId) {
    }
}