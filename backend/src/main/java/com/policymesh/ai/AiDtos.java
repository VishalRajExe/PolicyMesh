package com.policymesh.ai;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class AiDtos {
  private AiDtos() {}

  /** sampleValue is optional metadata; only field names/metadata are ever sent to the AI service. */
  public record Request(@NotBlank String fieldName, String sampleValue) {}

  public record Response(Long id, String fieldName, String suggestedClass, double confidence,
                         String status, String provider, String reviewedBy, Instant createdAt) {}

  static Response from(AIClassification a) {
    return new Response(a.getId(), a.getFieldName(), a.getClassification(), a.getConfidence(),
        a.getStatus(), a.getProvider(), a.getReviewedBy(), a.getCreatedAt());
  }
}
