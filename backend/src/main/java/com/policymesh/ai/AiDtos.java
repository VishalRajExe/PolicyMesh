package com.policymesh.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class AiDtos {
  private AiDtos() {}

  /** sampleValue is optional metadata; only field names/metadata are ever sent to the AI service. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Request(
      @NotBlank @Size(min = 1, max = 255)
      @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "fieldName must be alphanumeric with dots, underscores, or hyphens")
      String fieldName,

      @Size(max = 500, message = "sampleValue must not exceed 500 characters")
      String sampleValue) {}

  public record Response(Long id, String fieldName, String suggestedClass, double confidence,
                         String status, String provider, String reviewedBy, Instant createdAt) {}

  static Response from(AIClassification a) {
    return new Response(a.getId(), a.getFieldName(), a.getClassification(), a.getConfidence(),
        a.getStatus(), a.getProvider(), a.getReviewedBy(), a.getCreatedAt());
  }
}
