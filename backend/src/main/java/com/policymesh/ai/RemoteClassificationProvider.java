package com.policymesh.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Calls the external Python AI service (POST {AI_SERVICE_URL}/classify). When the service is
 * unreachable or misbehaves it degrades to the local provider instead of failing the request —
 * classification is assistive, never a hard dependency.
 */
@Component
public class RemoteClassificationProvider implements ClassificationProvider {
  private final RestClient client;
  private final LocalClassificationProvider fallback;
  private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RemoteClassificationProvider.class);

  public RemoteClassificationProvider(@Value("${policymesh.ai.service-url:http://localhost:8000}") String serviceUrl,
                                      LocalClassificationProvider fallback) {
    String normalizedUrl = (serviceUrl != null && !serviceUrl.isBlank()) ? serviceUrl.trim() : "http://localhost:8000";
    if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
      normalizedUrl = "http://" + normalizedUrl;
    }
    this.client = RestClient.builder()
        .baseUrl(normalizedUrl)
        .requestFactory(factoryWithTimeouts())
        .build();
    this.fallback = fallback;
  }

  @Override
  public Result classify(String fieldName, String sampleValue) {
    try {
      java.util.Map<String, Object> fieldItem = new java.util.HashMap<>();
      fieldItem.put("name", fieldName == null ? "" : fieldName);
      if (sampleValue != null && !sampleValue.isBlank()) {
        fieldItem.put("sampleValue", sampleValue);
      }
      JsonNode response = client.post()
          .uri("/api/v1/classify")
          .body(java.util.Map.of("fields", java.util.List.of(fieldItem)))
          .retrieve()
          .body(JsonNode.class);
      if (response != null) {
        JsonNode classifications = response.path("classifications");
        JsonNode first = (classifications.isArray() && classifications.size() > 0) ? classifications.get(0) : response;
        String classification = first.hasNonNull("classification") ? first.get("classification").asText()
            : (first.hasNonNull("class") ? first.get("class").asText() : null);
        if (classification != null && !classification.isBlank()) {
          double confidence = first.path("confidence").asDouble(0.9);
          return new Result(classification.toUpperCase(), confidence, "remote");
        }
      }
      log.warn("AI service returned an unusable response; falling back to local classification");
    } catch (RuntimeException e) {
      log.warn("AI service unreachable ({}); falling back to local classification", e.getMessage());
    }
    return fallback.classify(fieldName, sampleValue);
  }

  @Override
  public String describe() { return "remote AI service with local fallback"; }

  private static org.springframework.http.client.ClientHttpRequestFactory factoryWithTimeouts() {
    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
    factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
    return factory;
  }
}
