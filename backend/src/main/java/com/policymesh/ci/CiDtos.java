package com.policymesh.ci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.policymesh.graph.GraphModels;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class CiDtos {
  private CiDtos() {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Request(
      @NotBlank @Size(min = 1, max = 64)
      @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "commitHash must be alphanumeric with dots or hyphens")
      String commitHash,

      @NotBlank @Size(min = 1, max = 255)
      @Pattern(regexp = "^[a-zA-Z0-9/_.-]+$", message = "branch contains invalid characters")
      String branch) {}

  /** result is PASS or FAIL; a FAIL is HTTP 200 — compliance failures are business results, not server errors. */
  public record Response(Long id, String commitHash, String branch, String result, int violationCount,
                         Instant startedAt, Instant completedAt, List<GraphModels.Violation> violations,
                         String humanReadable) {
    public boolean passed() { return "PASS".equals(result); }
  }

  static Response from(CIScan s, List<GraphModels.Violation> violations) {
    return new Response(s.getId(), s.getCommitHash(), s.getBranch(), s.getStatus(), s.getViolationCount(),
        s.getStartedAt(), s.getCompletedAt(), violations, humanSummary(s));
  }

  static String humanSummary(CIScan s) {
    return "PolicyMesh CI " + s.getStatus() + ": " + s.getViolationCount() + " violation(s) on branch "
        + s.getBranch() + " @ " + s.getCommitHash();
  }
}
