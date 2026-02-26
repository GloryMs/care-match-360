package com.careprofileservice.service;


import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.presigned-url-expiration}")
    private long presignedUrlExpiration;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo"
    );

    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public String uploadFile(MultipartFile file, String folder, String fileType) {
        validateFile(file, fileType);

        String fileName = generateFileName(file.getOriginalFilename());
        String key = folder + "/" + fileName;

        try {
            S3Client s3Client = getS3Client();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            log.info("File uploaded successfully: {}", key);
            return key;

        } catch (IOException e) {
            log.error("Failed to upload file: {}", key, e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public String generatePresignedUrl(String key) {
        try {
            S3Presigner presigner = getS3Presigner();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(presignedUrlExpiration))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

            log.debug("Generated presigned URL for key: {}", key);
            return presignedRequest.url().toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", key, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    private void validateFile(MultipartFile file, String fileType) {
        if (file.isEmpty()) {
            throw new ValidationException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("File size exceeds maximum allowed size of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new ValidationException("File content type is unknown");
        }

        boolean isValid = switch (fileType.toLowerCase()) {
            case "image" -> ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
            case "video" -> ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase());
            case "document" -> ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase());
            default -> false;
        };

        if (!isValid) {
            throw new ValidationException("File type not allowed: " + contentType);
        }
    }

    private String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private AwsCredentialsProvider buildCredentialsProvider() {
        log.info("Building AWS Credentials Provider - accessKey: {}, secretKey: {}", accessKey, secretKey);
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.create();
    }

    private S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(buildCredentialsProvider())
                .build();
    }

    private S3Presigner getS3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(buildCredentialsProvider())
                .build();
    }

    public String getPublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }
}
