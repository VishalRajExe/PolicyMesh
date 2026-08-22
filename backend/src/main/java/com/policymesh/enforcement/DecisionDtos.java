package com.policymesh.enforcement;

import java.time.Instant;
import java.util.List;

public final class DecisionDtos {
  private DecisionDtos() {}

  public record Response(Long id, String sourceService, String destinationService,
                         String sourceRegion, String destinationRegion, String dataClass,
                         String decision, String policyId, String reason, Instant createdAt) {}

  public static Response from(DecisionRecord d) {
    return new Response(d.getId(), d.getSourceService(), d.getDestinationService(),
        d.getSourceRegion(), d.getDestinationRegion(), d.getDataClass(),
        d.getDecision(), d.getPolicyId(), d.getReason(), d.getCreatedAt());
  }

  public static List<Response> from(Iterable<DecisionRecord> decisions) {
    var result = new java.util.ArrayList<Response>();
    for (DecisionRecord d : decisions) result.add(from(d));
    return result;
  }
}
