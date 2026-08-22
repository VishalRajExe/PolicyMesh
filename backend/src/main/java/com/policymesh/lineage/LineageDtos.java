package com.policymesh.lineage;

import java.time.Instant;

public final class LineageDtos {
  private LineageDtos() {}

  public record Response(Long id, Long decisionId, String sourceService, String destinationService,
                         String sourceRegion, String destinationRegion, String dataClass,
                         String decision, String policyId, String reason,
                         String previousHash, String currentHash, Instant createdAt) {}

  /** brokenAt is the id of the first record whose linkage or content fails verification. */
  public record Verification(boolean valid, Long brokenAt, long recordsChecked, String detail) {}

  static Response from(LineageRecord r) {
    return new Response(r.getId(), r.getDecisionId(), r.getSourceService(), r.getDestinationService(),
        r.getSourceRegion(), r.getDestinationRegion(), r.getDataClass(), r.getDecision(),
        r.getPolicyId(), r.getReason(), r.getPreviousHash(), r.getCurrentHash(), r.getCreatedAt());
  }
}
