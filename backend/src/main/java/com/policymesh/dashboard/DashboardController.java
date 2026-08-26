package com.policymesh.dashboard;

import com.policymesh.enforcement.DecisionRepository;
import com.policymesh.graph.GraphAnalyzer;
import com.policymesh.lineage.LineageService;
import com.policymesh.policy.PolicyRepository;
import com.policymesh.servicegraph.ServiceNodeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
  private final PolicyRepository policies;
  private final ServiceNodeRepository services;
  private final DecisionRepository decisions;
  private final GraphAnalyzer graph;
  private final LineageService lineage;

  public DashboardController(PolicyRepository policies, ServiceNodeRepository services,
                             DecisionRepository decisions, GraphAnalyzer graph, LineageService lineage) {
    this.policies = policies;
    this.services = services;
    this.decisions = decisions;
    this.graph = graph;
    this.lineage = lineage;
  }

  public record Summary(double complianceScore, long totalPolicies, long totalServices,
                        long allowedTransfers, long blockedTransfers, long activeViolations,
                        long decisionsToday, boolean lineageValid) {}

  @GetMapping("/summary")
  public Summary summary() {
    long violations = graph.validate().violationCount();
    long allowed = decisions.countByDecision("ALLOW");
    long blocked = decisions.countByDecision("DENY");
    long total = allowed + blocked;
    double score = total == 0 ? 0.0 : Math.round(allowed * 1000.0 / total) / 10.0;
    Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
    return new Summary(score, policies.count(), services.count(), allowed, blocked, violations,
        decisions.countByCreatedAtAfter(startOfDay), lineage.verify().valid());
  }
}
