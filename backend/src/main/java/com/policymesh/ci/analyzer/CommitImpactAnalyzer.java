package com.policymesh.ci.analyzer;

import com.policymesh.ci.CiDtos;
import com.policymesh.ci.git.ChangedFile;
import com.policymesh.ci.git.CommitInfo;
import com.policymesh.graph.GraphAnalyzer;
import com.policymesh.graph.GraphModels;
import com.policymesh.policy.Decision;
import com.policymesh.policy.PolicyEngine;
import com.policymesh.policy.PolicyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommitImpactAnalyzer {
  private final GraphAnalyzer graphAnalyzer;
  private final PolicyEngine policyEngine;
  private final PolicyRepository policyRepository;

  public CommitImpactAnalyzer(
      GraphAnalyzer graphAnalyzer,
      PolicyEngine policyEngine,
      PolicyRepository policyRepository
  ) {
    this.graphAnalyzer = graphAnalyzer;
    this.policyEngine = policyEngine;
    this.policyRepository = policyRepository;
  }

  public record AnalysisResult(
      int totalFilesAnalyzed,
      int flowsChecked,
      int passedFlows,
      int failedFlows,
      List<CiDtos.ViolationDetail> violations
  ) {}

  public AnalysisResult analyze(CommitInfo commit) {
    GraphModels.CheckResult baseline = graphAnalyzer.validate();
    List<GraphModels.Violation> baseViolations = baseline.violations();

    List<CiDtos.ViolationDetail> richViolations = new ArrayList<>();
    int totalFiles = commit.changedFiles() != null ? commit.changedFiles().size() : 1;
    int flowsChecked = Math.max(baseline.checkedEdges(), 1);

    String primaryChangedFile = "services/service-graph.json";
    if (commit.changedFiles() != null && !commit.changedFiles().isEmpty()) {
      for (ChangedFile f : commit.changedFiles()) {
        if (f.category() == com.policymesh.ci.git.ChangedFileCategory.DATAFLOW ||
            f.category() == com.policymesh.ci.git.ChangedFileCategory.SERVICE ||
            f.category() == com.policymesh.ci.git.ChangedFileCategory.POLICY) {
          primaryChangedFile = f.path();
          break;
        }
      }
    }

    if (baseViolations != null && !baseViolations.isEmpty()) {
      for (GraphModels.Violation v : baseViolations) {
        String policyCode = v.policyCode() != null ? v.policyCode() : "EU-PII-001";
        String policyName = resolvePolicyName(policyCode);
        String reason = v.reason() != null ? v.reason() : ("Destination region " + v.destinationRegion() + " is not permitted for " + v.dataClass() + " under active policy " + policyCode);
        String whatChanged = "+ Proposed data flow: " + v.sourceService() + " (" + v.sourceRegion() + ") → " + v.destinationService() + " (" + v.destinationRegion() + ") [" + v.dataClass() + "]";
        String howToFix = "Option 1: Deploy '" + v.destinationService() + "' in compliant region '" + v.sourceRegion() + "'. Option 2: Remove sensitive '" + v.dataClass() + "' classification from this data transfer. Option 3: Route via an approved regional proxy.";

        List<String> visualFlow = List.of(
            "Commit: " + commit.shortSha(),
            "File: " + primaryChangedFile,
            v.sourceService() + " [" + v.sourceRegion() + "]",
            v.destinationService() + " [" + v.destinationRegion() + "]",
            "Data: " + v.dataClass(),
            "Policy: " + policyCode,
            "DENY",
            "MERGE BLOCKED"
        );

        CiDtos.BeforeAfterFlow beforeAfter = new CiDtos.BeforeAfterFlow(
            new CiDtos.FlowState(v.sourceService() + " [" + v.sourceRegion() + "]", "payments-api [" + v.sourceRegion() + "]", "ALLOW"),
            new CiDtos.FlowState(v.sourceService() + " [" + v.sourceRegion() + "]", v.destinationService() + " [" + v.destinationRegion() + "]", "DENY")
        );

        richViolations.add(new CiDtos.ViolationDetail(
            v.sourceService(),
            v.sourceRegion(),
            v.destinationService(),
            v.destinationRegion(),
            v.dataClass(),
            policyCode,
            policyName,
            reason,
            whatChanged,
            howToFix,
            visualFlow,
            beforeAfter
        ));
      }
    }

    int failedFlows = richViolations.size();
    int passedFlows = Math.max(0, flowsChecked - failedFlows);

    return new AnalysisResult(
        totalFiles,
        flowsChecked,
        passedFlows,
        failedFlows,
        richViolations
    );
  }

  private String resolvePolicyName(String policyCode) {
    if (policyCode == null) return "Data Residency & Sovereignty Policy";
    return policyRepository.findByPolicyCodeIgnoreCase(policyCode)
        .map(com.policymesh.policy.Policy::getName)
        .orElse(policyCode.startsWith("EU") ? "EU GDPR Data Residency Protection" :
               policyCode.startsWith("IN") ? "India DPDP Cross-Border Restriction" :
               policyCode.startsWith("US") ? "US Privacy Data Restriction" :
               "Cross-Border Compliance Rule (" + policyCode + ")");
  }
}
