package com.policymesh.ci;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.ci.analyzer.CommitImpactAnalyzer;
import com.policymesh.ci.git.ChangedFile;
import com.policymesh.ci.git.CommitInfo;
import com.policymesh.ci.git.GitProvider;
import com.policymesh.common.ApiException;
import com.policymesh.events.EventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CiService {
  private final CIScanRepository scans;
  private final GitProvider gitProvider;
  private final CommitImpactAnalyzer impactAnalyzer;
  private final com.policymesh.enforcement.DecisionRepository decisions;
  private final EventPublisher events;
  private final ObjectMapper mapper;

  public CiService(
      CIScanRepository scans,
      GitProvider gitProvider,
      CommitImpactAnalyzer impactAnalyzer,
      com.policymesh.enforcement.DecisionRepository decisions,
      EventPublisher events,
      ObjectMapper mapper
  ) {
    this.scans = scans;
    this.gitProvider = gitProvider;
    this.impactAnalyzer = impactAnalyzer;
    this.decisions = decisions;
    this.events = events;
    this.mapper = mapper;
  }

  public CiDtos.Response run(String branch, String commitHash) {
    // 1. Authoritative Git verification (validates branch existence, commit existence, reachability)
    CommitInfo commit = gitProvider.getCommit(branch, commitHash);

    // 2. Perform deep impact analysis and active policy evaluation
    CommitImpactAnalyzer.AnalysisResult analysis = impactAnalyzer.analyze(commit);

    boolean passed = analysis.failedFlows() == 0 && analysis.violations().isEmpty();
    String status = passed ? "PASS" : "FAIL";

    // 3. Persist scan result
    CIScan scan = new CIScan();
    scan.setBranch(branch);
    scan.setCommitHash(commit.fullSha());
    scan.setStatus(status);
    scan.setViolationCount(analysis.violations().size());
    scan.setCommitMessage(commit.message());
    scan.setAuthor(commit.authorName());
    scan.setParentSha(commit.parentSha());
    scan.setFlowsChecked(analysis.flowsChecked());
    scan.setPassedFlows(analysis.passedFlows());
    scan.setFailedFlows(analysis.failedFlows());
    scan.setViolationsJson(toJson(analysis.violations()));
    scan.setChangedFilesJson(toJson(commit.changedFiles()));
    scan.complete();
    scan = scans.save(scan);

    // 4. Publish telemetry event for asynchronous alerts and audit pipelines
    Map<String, Object> payload = new HashMap<>();
    payload.put("scanId", scan.getId());
    payload.put("result", scan.getStatus());
    payload.put("violationCount", scan.getViolationCount());
    payload.put("commitHash", commit.shortSha());
    payload.put("branch", branch);
    payload.put("violations", analysis.violations());
    events.publish(EventPublisher.TOPIC_CI_COMPLETED, payload);

    return CiDtos.from(
        scan,
        commit,
        analysis.totalFilesAnalyzed(),
        analysis.flowsChecked(),
        analysis.passedFlows(),
        analysis.failedFlows(),
        analysis.violations()
    );
  }

  @Transactional(readOnly = true)
  public List<String> listBranches() {
    return gitProvider.listBranches();
  }

  @Transactional(readOnly = true)
  public Page<CiDtos.Response> listScans(Pageable pageable) {
    return scans.findAllByOrderByStartedAtDesc(pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public CiDtos.Response one(long id) {
    CIScan scan = scans.findById(id).orElseThrow(() -> ApiException.notFound("CI scan not found"));
    return toResponse(scan);
  }

  private CiDtos.Response toResponse(CIScan s) {
    List<CiDtos.ViolationDetail> violations = fromJson(s.getViolationsJson(), new TypeReference<List<CiDtos.ViolationDetail>>() {});
    List<ChangedFile> files = fromJson(s.getChangedFilesJson(), new TypeReference<List<ChangedFile>>() {});

    CommitInfo commit = new CommitInfo(
        s.getCommitHash(),
        s.getCommitHash() != null && s.getCommitHash().length() > 7 ? s.getCommitHash().substring(0, 7) : s.getCommitHash(),
        s.getBranch(),
        s.getAuthor() != null ? s.getAuthor() : "Developer",
        "dev@policymesh.io",
        s.getCommitMessage() != null ? s.getCommitMessage() : ("Scan @" + s.getCommitHash()),
        s.getStartedAt(),
        s.getParentSha(),
        files
    );

    return CiDtos.from(
        s,
        commit,
        files.size(),
        s.getFlowsChecked() > 0 ? s.getFlowsChecked() : Math.max(1, violations.size()),
        s.getPassedFlows(),
        s.getFailedFlows() > 0 ? s.getFailedFlows() : violations.size(),
        violations
    );
  }

  private String toJson(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (java.io.IOException e) {
      return "[]";
    }
  }

  private <T> T fromJson(String json, TypeReference<T> typeRef) {
    try {
      if (json == null || json.isBlank()) {
        return mapper.readValue("[]", typeRef);
      }
      return mapper.readValue(json, typeRef);
    } catch (java.io.IOException e) {
      try {
        return mapper.readValue("[]", typeRef);
      } catch (Exception ignored) {
        return null;
      }
    }
  }
}
