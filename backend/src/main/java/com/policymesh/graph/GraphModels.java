package com.policymesh.graph;

import java.util.List;
import java.util.Set;

public final class GraphModels {
  private GraphModels() {}

  public record Node(Long id, String name, String region, String meshZone, String environment) {}

  public record Edge(Long id, Long sourceServiceId, Long destinationServiceId, Set<String> dataClasses) {}

  public record View(List<Node> nodes, List<Edge> edges) {}

  public record Violation(Long edgeId, String sourceService, String destinationService,
                          String sourceRegion, String destinationRegion,
                          String dataClass, String policyCode, String reason) {}

  /** result is PASS when no violations exist, FAIL otherwise; compliance failures are business results, never errors. */
  public record CheckResult(String result, int violationCount, int checkedEdges, List<Violation> violations) {
    public static CheckResult of(int checkedEdges, List<Violation> violations) {
      return new CheckResult(violations.isEmpty() ? "PASS" : "FAIL", violations.size(), checkedEdges, violations);
    }
    public boolean passed() { return "PASS".equals(result); }
  }
}
