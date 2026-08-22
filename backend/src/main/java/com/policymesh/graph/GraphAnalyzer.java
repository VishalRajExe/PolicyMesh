package com.policymesh.graph;

import com.policymesh.policy.Decision;
import com.policymesh.policy.PolicyEngine;
import com.policymesh.policy.PolicyEvaluation;
import com.policymesh.servicegraph.DataFlowEdge;
import com.policymesh.servicegraph.DataFlowEdgeRepository;
import com.policymesh.servicegraph.ServiceNode;
import com.policymesh.servicegraph.ServiceNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads services and edges, resolves regions and data classes, and delegates every decision
 * to the single PolicyEngine. Never re-implements policy rules itself.
 */
@Service
public class GraphAnalyzer {
  private final ServiceNodeRepository services;
  private final DataFlowEdgeRepository edges;
  private final PolicyEngine engine;

  public GraphAnalyzer(ServiceNodeRepository services, DataFlowEdgeRepository edges, PolicyEngine engine) {
    this.services = services;
    this.edges = edges;
    this.engine = engine;
  }

  @Transactional(readOnly = true)
  public GraphModels.View graph() {
    List<GraphModels.Node> nodes = services.findAll().stream()
        .map(s -> new GraphModels.Node(s.getId(), s.getName(), s.getRegion(), s.getMeshZone(), s.getEnvironment()))
        .toList();
    List<GraphModels.Edge> edgeList = edges.findAll().stream()
        .map(e -> new GraphModels.Edge(e.getId(), e.getSourceServiceId(), e.getDestinationServiceId(), Set.copyOf(e.getDataClasses())))
        .toList();
    return new GraphModels.View(nodes, edgeList);
  }

  @Transactional(readOnly = true)
  public GraphModels.CheckResult validate() {
    Map<Long, ServiceNode> nodes = new HashMap<>();
    services.findAll().forEach(s -> nodes.put(s.getId(), s));
    List<GraphModels.Violation> violations = new ArrayList<>();
    List<DataFlowEdge> allEdges = edges.findAll();
    for (DataFlowEdge edge : allEdges) {
      ServiceNode source = nodes.get(edge.getSourceServiceId());
      ServiceNode destination = nodes.get(edge.getDestinationServiceId());
      if (source == null || destination == null) {
        violations.add(new GraphModels.Violation(edge.getId(),
            source != null ? source.getName() : "unknown#" + edge.getSourceServiceId(),
            destination != null ? destination.getName() : "unknown#" + edge.getDestinationServiceId(),
            source != null ? source.getRegion() : "?",
            destination != null ? destination.getRegion() : "?",
            String.join(",", edge.getDataClasses()),
            null, "Edge references a service that no longer exists"));
        continue;
      }
      for (String dataClass : edge.getDataClasses()) {
        PolicyEvaluation evaluation = engine.evaluate(
            source.getName(), destination.getName(),
            source.getRegion(), destination.getRegion(),
            dataClass, edge.getDataClasses());
        if (evaluation.decision() != Decision.ALLOW) {
          violations.add(new GraphModels.Violation(edge.getId(), source.getName(), destination.getName(),
              source.getRegion(), destination.getRegion(), dataClass,
              evaluation.policyId(), evaluation.reason()));
        }
      }
    }
    return GraphModels.CheckResult.of(allEdges.size(), violations);
  }
}
