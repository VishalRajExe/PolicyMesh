package com.policymesh.ci;

import com.policymesh.graph.GraphModels;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public final class CiDtos {
  private CiDtos() {}

  public record Request(@NotBlank String commitHash, @NotBlank String branch) {}

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
