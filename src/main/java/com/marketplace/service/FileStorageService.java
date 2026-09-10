package com.marketplace.service;

import com.marketplace.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final String ALLOWED_EXTENSION = ".zip";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${app.download.token-expiry-ms}")
    private long downloadUrlExpiryMs;

    public UploadResult uploadProductFile(MultipartFile file, Long vendorId, Long productId) {
        validateFile(file);

        byte[] fileBytes = readBytes(file);
        String sha256 = computeSha256(fileBytes);

        String key = "products/%d/%d/%s.zip".formatted(vendorId, productId, UUID.randomUUID());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/zip")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(fileBytes));

        return new UploadResult(key, sha256);
    }

    public PresignedDownload generatePresignedDownloadUrl(String fileKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMillis(downloadUrlExpiryMs))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);

        Instant expiresAt = Instant.now().plusMillis(downloadUrlExpiryMs);
        return new PresignedDownload(presigned.url().toString(), expiresAt);
    }

    public record PresignedDownload(String url, Instant expiresAt) {
    }

    public void deleteFile(String fileKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A file is required");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(ALLOWED_EXTENSION)) {
            throw new BadRequestException("Only .zip files are allowed");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded file");
        }
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record UploadResult(String fileKey, String fileSha256) {
    }
}