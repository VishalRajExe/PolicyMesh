package com.policymesh.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class GraphModels {
  private GraphModels() {}

  public record Node(Long id, String name, String region, String meshZone, String environment) {}

  public record Edge(Long id, Long sourceServiceId, Long destinationServiceId, Set<String> dataClasses) {}

  public record View(List<Node> nodes, List<Edge> edges) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Violation(Long edgeId, String sourceService, String destinationService,
                          String sourceRegion, String destinationRegion,
                          String dataClass, String policyCode, String reason) {}

  /** result is PASS when no violations exist, FAIL otherwise; compliance failures are business results, never errors. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CheckResult(String result, int violationCount, int checkedEdges, int totalFlows,
                           int compliantFlows, List<Violation> violations, Instant evaluatedAt) {
    public static CheckResult of(int checkedEdges, List<Violation> violations) {
      String res = violations.isEmpty() ? "PASS" : "FAIL";
      int vCount = violations.size();
      int compliant = Math.max(0, checkedEdges - vCount);
      return new CheckResult(res, vCount, checkedEdges, checkedEdges, compliant, violations, Instant.now());
    }
    public boolean passed() { return "PASS".equals(result); }
  }
}
