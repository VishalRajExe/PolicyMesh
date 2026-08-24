package com.policymesh.ci.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.ci.CiDtos;
import com.policymesh.ci.git.ChangedFile;
import com.policymesh.ci.git.ChangedFileCategory;
import com.policymesh.ci.git.CommitInfo;
import com.policymesh.ci.git.GitProvider;
import com.policymesh.compiler.CompiledPolicy;
import com.policymesh.compiler.PolicyCompiler;
import com.policymesh.graph.GraphAnalyzer;
import com.policymesh.graph.GraphModels;
import com.policymesh.policy.Policy;
import com.policymesh.policy.PolicyEngine;
import com.policymesh.policy.PolicyRepository;
import com.policymesh.policy.PolicyRuleEvaluator;
import com.policymesh.servicegraph.DataFlowEdgeRepository;
import com.policymesh.servicegraph.ServiceNode;
import com.policymesh.servicegraph.ServiceNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Service
public class CommitImpactAnalyzer {
  private static final Logger log = LoggerFactory.getLogger(CommitImpactAnalyzer.class);

  private final GitProvider gitProvider;
  private final GraphAnalyzer graphAnalyzer;
  private final PolicyEngine policyEngine;
  private final PolicyRepository policyRepository;
  private final ServiceNodeRepository serviceNodeRepository;
  private final DataFlowEdgeRepository dataFlowEdgeRepository;
  private final ObjectMapper mapper;

  public CommitImpactAnalyzer(
      GitProvider gitProvider,
      GraphAnalyzer graphAnalyzer,
      PolicyEngine policyEngine,
      PolicyRepository policyRepository,
      ServiceNodeRepository serviceNodeRepository,
      DataFlowEdgeRepository dataFlowEdgeRepository,
      ObjectMapper mapper
  ) {
    this.gitProvider = gitProvider;
    this.graphAnalyzer = graphAnalyzer;
    this.policyEngine = policyEngine;
    this.policyRepository = policyRepository;
    this.serviceNodeRepository = serviceNodeRepository;
    this.dataFlowEdgeRepository = dataFlowEdgeRepository;
    this.mapper = mapper;
  }

  public record AnalysisResult(
      int totalFilesAnalyzed,
      int flowsChecked,
      int passedFlows,
      int failedFlows,
      List<CiDtos.ViolationDetail> violations
  ) {}

  private record FlowSpec(String source, String destination, List<String> dataClasses, String filePath) {}

  public AnalysisResult analyze(CommitInfo commit) {
    int totalFiles = commit.changedFiles() != null && !commit.changedFiles().isEmpty()
        ? commit.changedFiles().size()
        : 1;
    String fullSha = commit.fullSha();

    // 1. Resolve service -> region map from commit configuration and database
    Map<String, String> serviceRegions = resolveServicesAtCommit(fullSha);

    // 2. Load active compiled policies from workspace / commit
    List<CompiledPolicy> compiledPolicies = loadCompiledPolicies(fullSha);

    // 3. Resolve all data flows configured at this specific commit tree
    List<FlowSpec> flowsToCheck = resolveDataFlowsAtCommit(commit);

    List<CiDtos.ViolationDetail> richViolations = new ArrayList<>();
    Set<String> seenViolations = new HashSet<>();
    int flowsChecked = 0;

    String primaryChangedFile = "examples/dataflows/valid-flow.json";
    if (commit.changedFiles() != null && !commit.changedFiles().isEmpty()) {
      for (ChangedFile f : commit.changedFiles()) {
        if (f.category() == ChangedFileCategory.DATAFLOW ||
            f.category() == ChangedFileCategory.SERVICE ||
            f.category() == ChangedFileCategory.POLICY) {
          primaryChangedFile = f.path();
          break;
        }
      }
    }

    // 4. Evaluate each data flow at this commit using the zero-trust policy engine
    for (FlowSpec flow : flowsToCheck) {
      String src = flow.source();
      String dst = flow.destination();
      String srcRegion = serviceRegions.getOrDefault(src, "EU");
      String dstRegion = serviceRegions.getOrDefault(dst, "EU");

      for (String dataClass : flow.dataClasses()) {
        flowsChecked++;
        var eval = PolicyRuleEvaluator.evaluate(
            PolicyRuleEvaluator.applicable(compiledPolicies, srcRegion),
            srcRegion,
            dstRegion,
            dataClass
        );

        String decisionName = eval.decision() != null ? eval.decision().name() : "ALLOW";
        if ("DENY".equals(decisionName) || "REROUTE".equals(decisionName)) {
          String key = src + "->" + dst + ":" + dataClass;
          if (seenViolations.add(key)) {
            String policyCode = eval.policyId() != null ? eval.policyId() : "EU-PII-001";
            String policyName = resolvePolicyName(policyCode);
            String reason = eval.reason() != null
                ? eval.reason()
                : ("Destination region " + dstRegion + " is denied by policy " + policyCode);
            String whatChanged = "+ Proposed data flow: " + src + " (" + srcRegion + ") → " + dst + " (" + dstRegion + ") [" + dataClass + "]";
            String howFix = "1. Reroute the data flow to a compliant service in " + srcRegion + ".\n"
                + "2. Mask or tokenize sensitive '" + dataClass + "' data before cross-border transfer.\n"
                + "3. Update governance policy '" + policyCode + "' only when legitimately authorized.";

            List<String> visualFlow = List.of(
                "Commit: " + commit.shortSha(),
                "File: " + (flow.filePath() != null ? flow.filePath() : primaryChangedFile),
                src + " [" + srcRegion + "]",
                dst + " [" + dstRegion + "]",
                "Data: " + dataClass,
                "Policy: " + policyCode,
                "DENY",
                "MERGE BLOCKED"
            );

            CiDtos.BeforeAfterFlow beforeAfter = new CiDtos.BeforeAfterFlow(
                new CiDtos.FlowState(src + " [" + srcRegion + "]", "payments-api [" + srcRegion + "]", "ALLOW"),
                new CiDtos.FlowState(src + " [" + srcRegion + "]", dst + " [" + dstRegion + "]", "DENY")
            );

            richViolations.add(new CiDtos.ViolationDetail(
                src,
                srcRegion,
                dst,
                dstRegion,
                dataClass,
                policyCode,
                policyName,
                reason,
                whatChanged,
                howFix,
                visualFlow,
                beforeAfter
            ));
          }
        }
      }
    }

    // 5. Also evaluate graph baseline from database (captures runtime UI / API topology tests)
    GraphModels.CheckResult baseline = graphAnalyzer.validate();
    if (baseline.violations() != null) {
      for (GraphModels.Violation v : baseline.violations()) {
        String key = v.sourceService() + "->" + v.destinationService() + ":" + v.dataClass();
        if (seenViolations.add(key)) {
          flowsChecked++;
          String policyCode = v.policyCode() != null ? v.policyCode() : "EU-PII-001";
          String policyName = resolvePolicyName(policyCode);
          String reason = v.reason() != null ? v.reason() : ("Destination region " + v.destinationRegion() + " is not permitted for " + v.dataClass() + " under active policy " + policyCode);
          String whatChanged = "+ Proposed data flow: " + v.sourceService() + " (" + v.sourceRegion() + ") → " + v.destinationService() + " (" + v.destinationRegion() + ") [" + v.dataClass() + "]";
          String howFix = "1. Reroute the data flow to a compliant service in " + v.sourceRegion() + ".\n"
              + "2. Mask or tokenize sensitive '" + v.dataClass() + "' data before cross-border transfer.";

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
              howFix,
              visualFlow,
              beforeAfter
          ));
        }
      }
    }

    int totalChecked = Math.max(Math.max(flowsChecked, baseline.checkedEdges()), 1);
    int failedFlows = richViolations.size();
    int passedFlows = Math.max(0, totalChecked - failedFlows);

    return new AnalysisResult(
        totalFiles,
        totalChecked,
        passedFlows,
        failedFlows,
        richViolations
    );
  }

  private Map<String, String> resolveServicesAtCommit(String commitSha) {
    Map<String, String> serviceRegions = new LinkedHashMap<>();

    // 1. Try reading services.json at the exact commit from git
    List<String> serviceCandidates = List.of("examples/services/services.json", "examples/services.json", "services.json");
    for (String path : serviceCandidates) {
      String json = gitProvider.getFileContentAtCommit(commitSha, path);
      if (json != null && !json.isBlank()) {
        try {
          JsonNode root = mapper.readTree(json);
          JsonNode servicesArray = root.has("services") ? root.get("services") : root;
          if (servicesArray.isArray()) {
            for (JsonNode s : servicesArray) {
              String id = s.has("id") ? s.get("id").asText() : s.has("name") ? s.get("name").asText() : null;
              String reg = s.has("region") ? s.get("region").asText() : s.has("sourceRegion") ? s.get("sourceRegion").asText() : null;
              if (id != null && reg != null) {
                serviceRegions.put(id, reg);
              }
            }
            if (!serviceRegions.isEmpty()) return serviceRegions;
          }
        } catch (Exception e) {
          log.debug("Failed to parse services from commit at {}: {}", path, e.getMessage());
        }
      }
    }

    // 2. Database services fallback
    for (ServiceNode sn : serviceNodeRepository.findAll()) {
      if (sn.getName() != null && sn.getRegion() != null) {
        serviceRegions.put(sn.getName(), sn.getRegion());
      }
    }

    if (serviceRegions.isEmpty()) {
      serviceRegions.put("web-app", "EU");
      serviceRegions.put("orders-api", "EU");
      serviceRegions.put("payments-api", "EU");
      serviceRegions.put("analytics-api", "US");
      serviceRegions.put("customer-db", "EU");
    }

    return serviceRegions;
  }

  private List<FlowSpec> resolveDataFlowsAtCommit(CommitInfo commit) {
    List<FlowSpec> flows = new ArrayList<>();
    String commitSha = commit.fullSha();

    Set<String> flowPaths = new LinkedHashSet<>();

    // 1. Always evaluate the primary CI dataflow configuration file at this commit tree
    flowPaths.add("examples/dataflows/valid-flow.json");

    // 2. Add any additional data flow files changed in this specific commit
    if (commit.changedFiles() != null) {
      for (ChangedFile f : commit.changedFiles()) {
        String p = f.path().toLowerCase();
        if (p.endsWith(".json") && (p.contains("dataflow") || p.contains("flow") || p.contains("service-graph"))) {
          flowPaths.add(f.path());
        }
      }
    }

    for (String path : flowPaths) {
      String json = gitProvider.getFileContentAtCommit(commitSha, path);
      if (json != null && !json.isBlank()) {
        parseFlowsFromJson(json, path, flows);
      }
    }

    return flows;
  }

  private void parseFlowsFromJson(String json, String path, List<FlowSpec> targetList) {
    try {
      JsonNode root = mapper.readTree(json);
      JsonNode flowsArray = root.has("dataFlows") ? root.get("dataFlows") : root.has("edges") ? root.get("edges") : root;
      if (flowsArray.isArray()) {
        for (JsonNode f : flowsArray) {
          String src = f.has("source") ? f.get("source").asText() : f.has("sourceService") ? f.get("sourceService").asText() : null;
          String dst = f.has("destination") ? f.get("destination").asText() : f.has("destinationService") ? f.get("destinationService").asText() : null;
          List<String> dcs = new ArrayList<>();
          if (f.has("dataClasses") && f.get("dataClasses").isArray()) {
            for (JsonNode dc : f.get("dataClasses")) dcs.add(dc.asText());
          } else if (f.has("dataClass")) {
            dcs.add(f.get("dataClass").asText());
          }
          if (dcs.isEmpty()) dcs.add("PII");

          if (src != null && dst != null) {
            targetList.add(new FlowSpec(src, dst, dcs, path));
          }
        }
      }
    } catch (Exception e) {
      log.debug("Failed parsing flows from {}: {}", path, e.getMessage());
    }
  }

  private File findRepoRoot() {
    File current = new File(".").getAbsoluteFile();
    while (current != null) {
      if (new File(current, ".git").isDirectory() || new File(current, "policies").isDirectory()) {
        return current;
      }
      current = current.getParentFile();
    }
    return new File(".");
  }

  private List<CompiledPolicy> loadCompiledPolicies(String commitSha) {
    List<CompiledPolicy> list = new ArrayList<>();
    PolicyCompiler compiler = new PolicyCompiler();

    // 1. Try reading policy files from workspace / policies directory
    File root = findRepoRoot();
    File policiesRoot = new File(root, "policies");
    if (policiesRoot.isDirectory()) {
      for (String reg : List.of("EU", "GLOBAL", "INDIA", "US")) {
        File regDir = new File(policiesRoot, reg);
        if (regDir.isDirectory()) {
          try (Stream<Path> files = Files.walk(regDir.toPath())) {
            files.filter(p -> {
              String s = p.getFileName().toString().toLowerCase();
              return s.endsWith(".yaml") || s.endsWith(".yml");
            }).forEach(p -> {
              try {
                String content = Files.readString(p);
                list.add(compiler.compile(content));
              } catch (Exception ignored) {}
            });
          } catch (Exception ignored) {}
        }
      }
    }

    // 2. Database policies fallback
    if (list.isEmpty()) {
      for (Policy p : policyRepository.findAll()) {
        try {
          list.add(CompiledPolicy.from(p));
        } catch (Exception ignored) {}
      }
    }

    return list;
  }

  private String resolvePolicyName(String policyCode) {
    if (policyCode == null) return "Data Residency & Sovereignty Policy";
    return policyRepository.findByPolicyCodeIgnoreCase(policyCode)
        .map(Policy::getName)
        .orElse(policyCode.startsWith("EU") ? "EU GDPR Data Residency Protection" :
               policyCode.startsWith("IN") ? "India DPDP Cross-Border Restriction" :
               policyCode.startsWith("US") ? "US Privacy Data Restriction" :
               "Cross-Border Compliance Rule (" + policyCode + ")");
  }
}
