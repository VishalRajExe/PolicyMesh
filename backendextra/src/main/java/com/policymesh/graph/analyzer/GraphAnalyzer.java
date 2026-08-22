package com.policymesh.graph.analyzer;

import com.policymesh.enforcement.engine.PolicyDecisionResult;
import com.policymesh.enforcement.engine.PolicyEngine;
import com.policymesh.graph.model.GraphCheckResult;
import com.policymesh.graph.model.GraphViolation;
import com.policymesh.servicegraph.entity.DataFlowEdge;
import com.policymesh.servicegraph.entity.ServiceNode;
import com.policymesh.servicegraph.repository.DataFlowEdgeRepository;
import com.policymesh.servicegraph.repository.ServiceNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads the full service graph (nodes + edges), evaluates every edge/
 * data-class combination against the {@link PolicyEngine}, and produces a
 * {@link GraphCheckResult}. Used by both the CI checker (build-time) and
 * the ad-hoc /api/v1/graph/validate endpoint.
 */
@Component
@RequiredArgsConstructor
public class GraphAnalyzer {

    private final ServiceNodeRepository serviceNodeRepository;
    private final DataFlowEdgeRepository dataFlowEdgeRepository;
    private final PolicyEngine policyEngine;

    @Transactional(readOnly = true)
    public GraphCheckResult analyze() {
        List<ServiceNode> services = serviceNodeRepository.findAll();
        List<DataFlowEdge> edges = dataFlowEdgeRepository.findAll();

        List<GraphViolation> violations = new ArrayList<>();

        for (DataFlowEdge edge : edges) {
            ServiceNode source = edge.getSource();
            ServiceNode destination = edge.getDestination();

            for (String dataClass : edge.dataClassList()) {
                PolicyDecisionResult result = policyEngine.evaluate(
                        source.getRegion(), destination.getRegion(), dataClass,
                        source.getName(), destination.getName());

                if (result.decision() != PolicyDecisionResult.Decision.ALLOW) {
                    violations.add(new GraphViolation(
                            source.getName(), destination.getName(),
                            source.getRegion(), destination.getRegion(),
                            dataClass, result.policyId(), result.reason()));
                }
            }
        }

        return GraphCheckResult.of(services.size(), edges.size(), violations);
    }
}
