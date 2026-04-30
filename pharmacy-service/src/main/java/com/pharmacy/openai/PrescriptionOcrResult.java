package com.pharmacy.openai;

import lombok.*;
import java.util.List;
import java.util.Map;

/**
 * Value object returned by OpenAiPrescriptionService after OCR processing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionOcrResult {
    private boolean success;
    private List<String> medicineNames;                    // simple list: ["Amoxicillin", "Paracetamol"]
    private List<Map<String, String>> medicineDetails;     // [{name, dosage, frequency, duration}]
    private String rawOpenAiResponse;
    private String errorMessage;

    public static PrescriptionOcrResult success(List<String> names,
                                                List<Map<String, String>> details,
                                                String rawResponse) {
        return PrescriptionOcrResult.builder()
                .success(true)
                .medicineNames(names)
                .medicineDetails(details)
                .rawOpenAiResponse(rawResponse)
                .build();
    }

    public static PrescriptionOcrResult failure(String error) {
        return PrescriptionOcrResult.builder()
                .success(false)
                .errorMessage(error)
                .build();
    }
}
