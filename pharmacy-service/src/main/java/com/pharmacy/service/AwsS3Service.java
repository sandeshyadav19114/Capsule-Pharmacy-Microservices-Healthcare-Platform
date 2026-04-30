package com.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * AwsS3Service — handles all S3 operations for prescription images.
 *
 * Operations:
 *  - uploadPrescriptionImage: uploads MultipartFile to S3, returns object key
 *  - generatePresignedUrl:    generates a 1-hour pre-signed URL for secure access
 *  - deleteImage:             soft-delete (used when prescription is invalidated)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AwsS3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Uploads prescription image to S3.
     * Key format: prescriptions/{patientId}/{uuid}.{extension}
     *
     * @return S3 object key (stored in DB for future URL generation)
     */
    public String uploadPrescriptionImage(MultipartFile file, Long patientId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";

        String key = String.format("prescriptions/%d/%s%s",
                patientId, UUID.randomUUID(), extension);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
        log.info("Uploaded prescription image to S3: bucket={} key={}", bucketName, key);
        return key;
    }

    /**
     * Generates a pre-signed URL valid for 60 minutes.
     * Use this URL to call OpenAI Vision API (it requires a public URL).
     */
    public String generatePresignedUrl(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(req -> req.bucket(bucketName).key(s3Key))
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();
        log.debug("Generated pre-signed URL for key: {}", s3Key);
        return url;
    }

    /**
     * Deletes an image from S3 (used for invalid/expired prescriptions).
     */
    public void deleteImage(String s3Key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build());
        log.info("Deleted S3 object: {}", s3Key);
    }
}
