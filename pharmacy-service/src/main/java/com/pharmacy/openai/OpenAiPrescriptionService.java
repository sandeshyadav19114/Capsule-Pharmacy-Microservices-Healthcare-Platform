package com.pharmacy.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OpenAiPrescriptionService — integrates with OpenAI GPT-4 Vision API
 * to automatically extract medicine names from uploaded prescription images.
 *
 * Flow:
 *  1. Prescription image is uploaded to AWS S3 (by PrescriptionService)
 *  2. S3 URL is sent to this service
 *  3. GPT-4 Vision analyses the image and returns structured JSON
 *  4. Medicine names are parsed and returned as a List<String>
 *  5. These names are matched against the Medicine catalog
 *
 * Prompt Engineering:
 *  - Explicitly asks for JSON-only response (no prose)
 *  - Handles handwritten and printed prescriptions
 *  - Extracts: medicine name, dosage, frequency
 */
@Service
@Slf4j
public class OpenAiPrescriptionService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    @Value("${openai.api.max-tokens}")
    private int maxTokens;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extracts medicine names from a prescription image URL (S3 pre-signed URL).
     *
     * @param imageUrl publicly accessible URL of the prescription image
     * @return PrescriptionOcrResult containing extracted medicines and raw response
     */
    public PrescriptionOcrResult extractMedicinesFromPrescription(String imageUrl) {
        log.info("Calling OpenAI Vision API for prescription OCR | imageUrl: {}", imageUrl);

        try {
            String requestBody = buildRequestBody(imageUrl);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("OpenAI API error: HTTP {}", response.code());
                    return PrescriptionOcrResult.failure("OpenAI API returned: " + response.code());
                }

                String responseBody = response.body().string();
                log.debug("OpenAI raw response: {}", responseBody);
                return parseOpenAiResponse(responseBody);
            }

        } catch (IOException e) {
            log.error("Failed to call OpenAI API: {}", e.getMessage());
            return PrescriptionOcrResult.failure("Network error: " + e.getMessage());
        }
    }

    /**
     * Builds the JSON request body for GPT-4 Vision API.
     * Prompt instructs the model to return ONLY JSON — critical for reliable parsing.
     */
    private String buildRequestBody(String imageUrl) throws IOException {
        String prompt = """
            You are a medical prescription parser. Analyse the prescription image and extract all medicines.
            
            Return ONLY a valid JSON object in this exact format (no explanation, no markdown):
            {
              "medicines": [
                {
                  "name": "Medicine Name",
                  "dosage": "500mg",
                  "frequency": "twice daily",
                  "duration": "7 days"
                }
              ],
              "doctorName": "Dr. Name if visible",
              "patientName": "Patient name if visible",
              "prescriptionDate": "date if visible"
            }
            
            If a field is not visible, use null. Extract every medicine listed, including generic names.
            """;

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", List.of(
            Map.of("type", "text", "text", prompt),
            Map.of("type", "image_url", "image_url", Map.of("url", imageUrl, "detail", "high"))
        ));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(message));
        requestBody.put("max_tokens", maxTokens);

        return objectMapper.writeValueAsString(requestBody);
    }

    /**
     * Parses the OpenAI response JSON and extracts medicine names.
     */
    private PrescriptionOcrResult parseOpenAiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText();

            log.debug("OpenAI extracted content: {}", content);

            // Parse the structured JSON from GPT
            JsonNode parsed = objectMapper.readTree(content);
            JsonNode medicinesNode = parsed.path("medicines");

            List<String> medicineNames = new ArrayList<>();
            List<Map<String, String>> medicineDetails = new ArrayList<>();

            if (medicinesNode.isArray()) {
                for (JsonNode med : medicinesNode) {
                    String name = med.path("name").asText(null);
                    if (name != null && !name.isBlank()) {
                        medicineNames.add(name.trim());
                        Map<String, String> detail = new HashMap<>();
                        detail.put("name", name.trim());
                        detail.put("dosage", med.path("dosage").asText("N/A"));
                        detail.put("frequency", med.path("frequency").asText("N/A"));
                        detail.put("duration", med.path("duration").asText("N/A"));
                        medicineDetails.add(detail);
                    }
                }
            }

            log.info("OpenAI extracted {} medicines: {}", medicineNames.size(), medicineNames);
            return PrescriptionOcrResult.success(medicineNames, medicineDetails, content);

        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage());
            return PrescriptionOcrResult.failure("Parse error: " + e.getMessage());
        }
    }
}
