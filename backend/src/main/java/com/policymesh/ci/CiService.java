package com.policymesh.ci;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.common.ApiException;
import com.policymesh.events.EventPublisher;
import com.policymesh.graph.GraphAnalyzer;
import com.policymesh.graph.GraphModels;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** CI Checker -> Graph Analyzer -> Policy Engine. Persists every scan with its violations. */
@Service
@Transactional
public class CiService {
  private final CIScanRepository scans;
  private final GraphAnalyzer graph;
  private final EventPublisher events;
  private final ObjectMapper mapper;

  public CiService(CIScanRepository scans, GraphAnalyzer graph, EventPublisher events, ObjectMapper mapper) {
    this.scans = scans;
    this.graph = graph;
    this.events = events;
    this.mapper = mapper;
  }

  public CiDtos.Response run(String branch, String commitHash) {
    GraphModels.CheckResult result = graph.validate();
    CIScan scan = new CIScan();
    scan.setBranch(branch);
    scan.setCommitHash(commitHash);
    scan.setStatus(result.result());
    scan.setViolationCount(result.violationCount());
    scan.setViolationsJson(toJson(result.violations()));
    scan.complete();
    scan = scans.save(scan);

    Map<String, Object> payload = new HashMap<>();
    payload.put("scanId", scan.getId());
    payload.put("result", scan.getStatus());
    payload.put("violationCount", scan.getViolationCount());
    events.publish(EventPublisher.TOPIC_CI_COMPLETED, payload);
    return CiDtos.from(scan, result.violations());
  }

  @Transactional(readOnly = true)
  public CiDtos.Response one(long id) {
    CIScan scan = scans.findById(id).orElseThrow(() -> ApiException.notFound("CI scan not found"));
    return CiDtos.from(scan, fromJson(scan.getViolationsJson()));
  }

  private String toJson(List<GraphModels.Violation> violations) {
    try {
      return mapper.writeValueAsString(violations);
    } catch (java.io.IOException e) {
      return "[]";
    }
  }

  private List<GraphModels.Violation> fromJson(String json) {
    try {
      return mapper.readValue(json == null ? "[]" : json, new TypeReference<List<GraphModels.Violation>>() {});
    } catch (java.io.IOException e) {
      return List.of();
    }
  }
}
