package com.policymesh.report;

import com.policymesh.ai.AIClassificationRepository;
import com.policymesh.enforcement.DecisionRecord;
import com.policymesh.enforcement.DecisionRepository;
import com.policymesh.graph.GraphAnalyzer;
import com.policymesh.lineage.LineageService;
import com.policymesh.policy.Policy;
import com.policymesh.policy.PolicyRepository;
import com.policymesh.servicegraph.ServiceNodeRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

  private final PolicyRepository policies;
  private final ServiceNodeRepository services;
  private final DecisionRepository decisions;
  private final AIClassificationRepository aiRepo;
  private final GraphAnalyzer graph;
  private final LineageService lineage;

  public ReportController(
      PolicyRepository policies,
      ServiceNodeRepository services,
      DecisionRepository decisions,
      AIClassificationRepository aiRepo,
      GraphAnalyzer graph,
      LineageService lineage
  ) {
    this.policies = policies;
    this.services = services;
    this.decisions = decisions;
    this.aiRepo = aiRepo;
    this.graph = graph;
    this.lineage = lineage;
  }

  @GetMapping("/compliance")
  public ReportDtos.ComplianceReport getComplianceReport() {
    List<Policy> allPolicies = policies.findAll();
    long totalPolicies = allPolicies.size();
    long activePolicies = allPolicies.stream()
        .filter(p -> p.getStatus() == com.policymesh.policy.PolicyStatus.ACTIVE)
        .count();
    long totalServices = services.count();

    long allowed = decisions.countByDecision("ALLOW");
    long blocked = decisions.countByDecision("DENY");
    long totalEvaluations = allowed + blocked;
    long activeViolations = graph.validate().violationCount();

    double complianceScore = totalEvaluations == 0
        ? (activeViolations == 0 ? 100.0 : 0.0)
        : Math.round(allowed * 1000.0 / totalEvaluations) / 10.0;

    Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
    long decisionsToday = decisions.countByCreatedAtAfter(startOfDay);

    ReportDtos.Summary summary = new ReportDtos.Summary(
        complianceScore,
        totalPolicies,
        activePolicies,
        totalServices,
        allowed,
        blocked,
        activeViolations,
        decisionsToday
    );

    // Policy breakdown
    List<DecisionRecord> recentDecisions = decisions.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 500));
    Map<String, Long> allowedByPolicy = new HashMap<>();
    Map<String, Long> blockedByPolicy = new HashMap<>();
    Map<String, Long> allowedByDataClass = new HashMap<>();
    Map<String, Long> blockedByDataClass = new HashMap<>();
    List<ReportDtos.ViolationItem> violations = new ArrayList<>();

    for (DecisionRecord d : recentDecisions) {
      String pol = d.getPolicyId() != null ? d.getPolicyId() : "UNSPECIFIED";
      String dc = d.getDataClass() != null ? d.getDataClass() : "UNKNOWN";
      if ("ALLOW".equalsIgnoreCase(d.getDecision())) {
        allowedByPolicy.merge(pol, 1L, Long::sum);
        allowedByDataClass.merge(dc, 1L, Long::sum);
      } else {
        blockedByPolicy.merge(pol, 1L, Long::sum);
        blockedByDataClass.merge(dc, 1L, Long::sum);
        if (violations.size() < 50) {
          violations.add(new ReportDtos.ViolationItem(
              d.getId(),
              d.getSourceService(),
              d.getSourceRegion(),
              d.getDestinationService(),
              d.getDestinationRegion(),
              d.getDataClass(),
              d.getPolicyId(),
              d.getReason(),
              d.getCreatedAt()
          ));
        }
      }
    }

    List<ReportDtos.PolicyAuditItem> policyBreakdown = allPolicies.stream().map(p -> new ReportDtos.PolicyAuditItem(
        p.getId(),
        p.getPolicyCode(),
        p.getName(),
        p.getJurisdiction(),
        p.getDataClass(),
        p.getAllowedRegions(),
        p.getDeniedRegions(),
        p.getStatus() != null ? p.getStatus().name() : "DRAFT",
        allowedByPolicy.getOrDefault(p.getPolicyCode(), 0L),
        blockedByPolicy.getOrDefault(p.getPolicyCode(), 0L)
    )).toList();

    List<String> knownClasses = List.of("PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN");
    List<ReportDtos.DataClassMetric> dataClassBreakdown = knownClasses.stream().map(dc -> {
      long a = allowedByDataClass.getOrDefault(dc, 0L);
      long b = blockedByDataClass.getOrDefault(dc, 0L);
      return new ReportDtos.DataClassMetric(dc, a + b, a, b);
    }).filter(m -> m.total() > 0 || "PII".equals(m.dataClass())).toList();

    // AI audit summary
    long aiTotal = aiRepo.count();
    long aiApproved = aiRepo.countByStatus("APPROVED");
    long aiPending = aiRepo.countByStatus("PENDING");
    long aiRejected = aiRepo.countByStatus("REJECTED");
    ReportDtos.AiAuditSummary aiSummary = new ReportDtos.AiAuditSummary(aiTotal, aiApproved, aiPending, aiRejected);

    // Lineage status
    var lineageVerify = lineage.verify();
    ReportDtos.LineageStatus lineageStatus = new ReportDtos.LineageStatus(
        lineageVerify.valid(),
        lineageVerify.recordsChecked(),
        "SHA-256",
        lineageVerify.valid() ? "SECURE & VERIFIED" : "INTEGRITY_COMPROMISED"
    );

    return new ReportDtos.ComplianceReport(
        Instant.now(),
        summary,
        policyBreakdown,
        dataClassBreakdown,
        aiSummary,
        lineageStatus,
        violations
    );
  }

  @GetMapping("/export/csv")
  public void exportCsv(HttpServletResponse response) throws IOException {
    response.setContentType("text/csv");
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"policymesh-compliance-report.csv\"");

    PrintWriter writer = response.getWriter();
    writer.println("Decision ID,Timestamp,Source Service,Source Region,Destination Service,Destination Region,Data Class,Decision,Policy ID,Reason");

    List<DecisionRecord> records = decisions.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 1000));
    for (DecisionRecord d : records) {
      writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
          d.getId(),
          d.getCreatedAt(),
          escapeCsv(d.getSourceService()),
          escapeCsv(d.getSourceRegion()),
          escapeCsv(d.getDestinationService()),
          escapeCsv(d.getDestinationRegion()),
          escapeCsv(d.getDataClass()),
          escapeCsv(d.getDecision()),
          escapeCsv(d.getPolicyId()),
          escapeCsv(d.getReason())
      );
    }
    writer.flush();
  }

  private String escapeCsv(String val) {
    if (val == null) return "";
    return val.replace("\"", "\"\"");
  }
}
