package com.policymesh.report;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class ReportDtos {
  private ReportDtos() {}

  public record ComplianceReport(
      Instant generatedAt,
      Summary summary,
      List<PolicyAuditItem> policyBreakdown,
      List<DataClassMetric> dataClassBreakdown,
      AiAuditSummary aiSummary,
      LineageStatus lineageStatus,
      List<ViolationItem> recentViolations
  ) {}

  public record Summary(
      double complianceScore,
      long totalPolicies,
      long activePolicies,
      long totalServices,
      long allowedTransfers,
      long blockedTransfers,
      long activeViolations,
      long decisionsToday
  ) {}

  public record PolicyAuditItem(
      Long id,
      String policyCode,
      String name,
      String jurisdiction,
      String dataClass,
      Set<String> allowedRegions,
      Set<String> deniedRegions,
      String status,
      long allowedEvaluations,
      long blockedEvaluations
  ) {}

  public record DataClassMetric(
      String dataClass,
      long total,
      long allowed,
      long blocked
  ) {}

  public record AiAuditSummary(
      long totalClassified,
      long approved,
      long pending,
      long rejected
  ) {}

  public record LineageStatus(
      boolean valid,
      long recordsChecked,
      String algorithm,
      String status
  ) {}

  public record ViolationItem(
      Long id,
      String sourceService,
      String sourceRegion,
      String destinationService,
      String destinationRegion,
      String dataClass,
      String policyId,
      String reason,
      Instant timestamp
  ) {}
}
