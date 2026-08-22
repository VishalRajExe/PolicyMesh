package com.policymesh.servicegraph.graph;

import com.policymesh.servicegraph.entity.DataFlowEdge;
import com.policymesh.servicegraph.entity.ServiceNode;
import com.policymesh.servicegraph.repository.DataFlowEdgeRepository;
import com.policymesh.servicegraph.repository.ServiceNodeRepository;
import com.policymesh.servicegraph.policy.engine.PolicyEngine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzes the service mesh graph for policy compliance.
 * Loads services and data flow edges, then evaluates each edge through the PolicyEngine.
 */
@Service
public class GraphAnalyzer {

    private final ServiceNodeRepository serviceNodeRepository;
    private final DataFlowEdgeRepository dataFlowEdgeRepository;
    private final PolicyEngine policyEngine;

    public GraphAnalyzer(ServiceNodeRepository serviceNodeRepository,
                         DataFlowEdgeRepository dataFlowEdgeRepository,
                         PolicyEngine policyEngine) {
        this.serviceNodeRepository = serviceNodeRepository;
        this.dataFlowEdgeRepository = dataFlowEdgeRepository;
        this.policyEngine = policyEngine;
    }

    /**
     * Performs a complete graph compliance analysis.
     *
     * @return GraphCheckResult containing the analysis status and any violations
     */
    public GraphCheckResult analyzeGraphCompliance() {
        // Load all active services
        List<ServiceNode> serviceNodes = serviceNodeRepository.findAll();
        Map<Long, GraphNode> nodes = new HashMap<>();
        for (ServiceNode serviceNode : serviceNodes) {
            nodes.put(serviceNode.getId(), new GraphNode(serviceNode));
        }

        // Load all data flow edges
        List<DataFlowEdge> dataFlowEdges = dataFlowEdgeRepository.findAll();
        List<GraphEdge> edges = new ArrayList<>();
        for (DataFlowEdge dataFlowEdge : dataFlowEdges) {
            edges.add(new GraphEdge(dataFlowEdge));
        }

        // Build the graph
        Graph graph = new Graph(nodes, edges);

        // Analyze each edge for policy compliance
        List<GraphViolation> violations = new ArrayList<>();
        for (GraphEdge edge : edges) {
            // Get source and destination service nodes
            GraphNode sourceNode = nodes.get(edge.getSourceServiceId());
            GraphNode destinationNode = nodes.get(edge.getDestinationServiceId());

            // Skip if either service node is not found (shouldn't happen with valid data)
            if (sourceNode == null || destinationNode == null) {
                continue;
            }

            // Check each data class on this edge against the policy engine
            for (String dataClass : edge.getDataClasses()) {
                // Create a policy decision context
                // Note: In a real implementation, we would create a proper PolicyRequest
                // based on the service nodes and data class, then evaluate it with the policy engine

                // For now, we'll simulate calling the policy engine
                // The actual implementation would depend on the PolicyEngine interface
                boolean isPermitted = checkPolicyPermission(
                        sourceNode,
                        destinationNode,
                        dataClass);

                if (!isPermitted) {
                    // Create a violation
                    GraphViolation violation = new GraphViolation(
                            sourceNode.getName(),
                            destinationNode.getName(),
                            sourceNode.getRegion(),
                            destinationNode.getRegion(),
                            dataClass,
                            "UNKNOWN_POLICY", // Would come from actual policy evaluation
                            "Data flow from " + sourceNode.getRegion() + " to " +
                                    destinationNode.getRegion() + " for " + dataClass +
                                    " is not permitted by policy");
                    violations.add(violation);
                }
            }
        }

        // Determine overall status
        String status = violations.isEmpty() ? "PASS" : "FAILED";
        return new GraphCheckResult(status, violations);
    }

    /**
     * Checks if a data flow is permitted by policy.
     * This is a simplified implementation - in reality, this would delegate
     * to the PolicyEngine to evaluate a proper policy request.
     */
    private boolean checkPolicyPermission(GraphNode sourceNode, GraphNode destinationNode, String dataClass) {
        // TODO: Implement actual policy engine integration
        // This would involve:
        // 1. Creating a PolicyRequest based on sourceNode, destinationNode, and dataClass
        // 2. Calling policyEngine.evaluate(request)
        // 3. Returning the result

        // For now, return true to allow compilation
        // Actual implementation will depend on the PolicyEngine interface
        return true;
    }

    /**
     * Returns the current service/data-flow graph for visualization or debugging.
     *
     * @return Graph representing the current service mesh
     */
    public Graph getGraph() {
        // Load all active services
        List<ServiceNode> serviceNodes = serviceNodeRepository.findAll();
        Map<Long, GraphNode> nodes = new HashMap<>();
        for (ServiceNode serviceNode : serviceNodes) {
            nodes.put(serviceNode.getId(), new GraphNode(serviceNode));
        }

        // Load all data flow edges
        List<DataFlowEdge> dataFlowEdges = dataFlowEdgeRepository.findAll();
        List<GraphEdge> edges = new ArrayList<>();
        for (DataFlowEdge dataFlowEdge : dataFlowEdges) {
            edges.add(new GraphEdge(dataFlowEdge));
        }

        return new Graph(nodes, edges);
    }
}