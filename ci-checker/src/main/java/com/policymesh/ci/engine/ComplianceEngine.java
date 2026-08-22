package com.policymesh.ci.engine;

import com.policymesh.ci.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Central compliance engine that orchestrates the full check.
 *
 * Workflow:
 * 1. Load policies.
 * 2. Load services.
 * 3. Load data-flow edges.
 * 4. Validate the graph structure.
 * 5. For each edge, for each data class, evaluate all applicable policies.
 * 6. If ANY mandatory policy denies the flow, it's a violation.
 * 7. Generate violations and return the final result.
 *
 * Deterministic behavior:
 * - Evaluate every applicable policy per data flow edge per data class.
 * - If ANY policy denies, the overall result for that flow is FAIL.
 * - If no policy denies (all ALLOW or NOT_APPLICABLE), the flow passes.
 */
public class ComplianceEngine {

    private final PolicyEvaluator evaluator;
    private final GraphAnalyzer graphAnalyzer;

    public ComplianceEngine() {
        this.evaluator = new PolicyEvaluator();
        this.graphAnalyzer = null;
    }

    public ComplianceEngine(PolicyEvaluator evaluator, GraphAnalyzer graphAnalyzer) {
        this.evaluator = evaluator;
        this.graphAnalyzer = graphAnalyzer;
    }

    /**
     * Runs a full compliance check.
     *
     * @param policies list of policies to check against
     * @param services list of services in the infrastructure
     * @param edges list of data-flow edges to check
     * @return ComplianceResult with status and all violations
     */
    public ComplianceResult check(List<Policy> policies, List<ServiceNode> services, List<DataFlowEdge> edges) {
        // Validate inputs
        if (policies == null || policies.isEmpty()) {
            return ComplianceResult.error("No policies loaded. At least one policy is required for compliance checking.");
        }
        if (services == null || services.isEmpty()) {
            return ComplianceResult.error("No services loaded. At least one service definition is required.");
        }
        if (edges == null || edges.isEmpty()) {
            // No data flows = nothing to check = pass
            ComplianceResult result = new ComplianceResult();
            result.setStatus(ComplianceResult.Status.PASSED);
            result.setTotalFlows(0);
            result.setPassedFlows(0);
            result.setFailedFlows(0);
            return result;
        }

        // Build service lookup
        GraphAnalyzer analyzer = graphAnalyzer != null ? graphAnalyzer : new GraphAnalyzer(services);

        // Validate graph structure
        try {
            analyzer.validate(edges);
        } catch (GraphValidationException e) {
            return ComplianceResult.error("Graph validation failed: " + e.getMessage());
        }

        // Check each edge
        List<ComplianceViolation> allViolations = new ArrayList<>();
        int passedFlows = 0;
        int failedFlows = 0;

        for (DataFlowEdge edge : edges) {
            ServiceNode source = analyzer.resolve(edge.getSource());
            ServiceNode destination = analyzer.resolve(edge.getDestination());

            // Skip if services couldn't be resolved (shouldn't happen after validation)
            if (source == null || destination == null) {
                continue;
            }

            boolean edgePassed = true;

            // Check each data class on this edge
            for (String dataClass : edge.getDataClasses()) {
                boolean dataClassDenied = false;
                List<ComplianceViolation> edgeViolations = new ArrayList<>();

                // Evaluate every applicable policy
                for (Policy policy : policies) {
                    CheckStatus decision = evaluator.evaluate(
                            policy, source.getRegion(), destination.getRegion(), dataClass);

                    if (decision == CheckStatus.DENY) {
                        dataClassDenied = true;
                        String reason = buildReason(policy, source.getRegion(), destination.getRegion(), dataClass);
                        edgeViolations.add(new ComplianceViolation(
                                source.getId(),
                                source.getRegion(),
                                destination.getId(),
                                destination.getRegion(),
                                dataClass,
                                policy.getId(),
                                policy.getName(),
                                reason,
                                ComplianceViolation.Severity.ERROR
                        ));
                    }
                }

                if (dataClassDenied) {
                    edgePassed = false;
                    allViolations.addAll(edgeViolations);
                }
            }

            if (edgePassed) {
                passedFlows++;
            } else {
                failedFlows++;
            }
        }

        // Build final result
        ComplianceResult result = new ComplianceResult();
        result.setTotalFlows(edges.size());
        result.setPassedFlows(passedFlows);
        result.setFailedFlows(failedFlows);
        result.setViolations(allViolations);
        result.setStatus(allViolations.isEmpty() ? ComplianceResult.Status.PASSED : ComplianceResult.Status.FAILED);

        return result;
    }

    /**
     * Builds a human-readable reason for a violation.
     */
    private String buildReason(Policy policy, String sourceRegion, String destinationRegion, String dataClass) {
        return String.format("%s %s cannot be transferred from %s to %s",
                policy.getJurisdiction() != null ? policy.getJurisdiction() : policy.getId(),
                dataClass,
                sourceRegion,
                destinationRegion);
    }
}
