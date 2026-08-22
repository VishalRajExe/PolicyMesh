package com.policymesh.ai.client;

import com.policymesh.ai.dto.FieldClassifyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Talks to an external Python AI classification service when
 * {@code policymesh.ai.service-url} is configured. If it is not
 * configured (or the call fails), falls back to a clean, deterministic
 * local mock so the rest of the backend stays runnable without the AI
 * service. Human approval is always required downstream before a
 * suggestion becomes enforcement-relevant — this client only produces
 * *suggestions*.
 */
@Slf4j
@Component
public class ClassificationClient {

    private final String serviceUrl;
    private final RestClient restClient = RestClient.create();

    public ClassificationClient(@Value("${policymesh.ai.service-url:}") String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public record Suggestion(String field, String sampleValue, String classification, double confidence) {
    }

    public List<Suggestion> classify(List<FieldClassifyRequest> fields) {
        if (serviceUrl != null && !serviceUrl.isBlank()) {
            try {
                return callExternalService(fields);
            } catch (Exception ex) {
                log.warn("External AI service call failed ({}); falling back to local mock classifier", ex.getMessage());
            }
        }
        return fields.stream()
                .map(f -> new Suggestion(f.name(), f.sampleValue(), localMockClassify(f), 0.75))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Suggestion> callExternalService(List<FieldClassifyRequest> fields) {
        Map<String, Object> response = restClient.post()
                .uri(serviceUrl)
                .body(Map.of("fields", fields))
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> classifications = (List<Map<String, Object>>) response.get("classifications");
        return classifications.stream()
                .map(c -> new Suggestion(
                        (String) c.get("field"),
                        null,
                        (String) c.get("classification"),
                        ((Number) c.getOrDefault("confidence", 0.5)).doubleValue()))
                .toList();
    }

    /**
     * Simple, transparent heuristic classifier used only when no external
     * AI service is configured. Not a substitute for a real model — it
     * exists purely so the rest of the platform (approval workflow,
     * enforcement wiring) remains testable end-to-end locally.
     */
    private String localMockClassify(FieldClassifyRequest field) {
        String name = field.name() == null ? "" : field.name().toLowerCase(Locale.ROOT);
        String sample = field.sampleValue() == null ? "" : field.sampleValue().toLowerCase(Locale.ROOT);

        if (name.contains("card") || name.contains("cvv") || sample.matches(".*\\d{12,19}.*")) {
            return "PCI";
        }
        if (name.contains("email") || name.contains("phone") || name.contains("address")
                || name.contains("name") || name.contains("ssn")) {
            return "PII";
        }
        if (name.contains("diagnosis") || name.contains("health") || name.contains("medical")) {
            return "PHI";
        }
        return "PUBLIC";
    }
}
