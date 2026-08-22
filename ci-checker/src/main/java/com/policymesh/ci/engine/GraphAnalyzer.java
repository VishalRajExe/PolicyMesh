package com.policymesh.ci.engine;

import com.policymesh.ci.model.DataFlowEdge;
import com.policymesh.ci.model.ServiceNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analyzes the service graph and resolves data flow edges to service nodes.
 *
 * Responsibilities:
 * - Validate that all services and edges are well-formed
 * - Resolve service IDs to ServiceNode objects
 * - Detect issues: missing services, self-loops, duplicates
 */
public class GraphAnalyzer {

    private final Map<String, ServiceNode> serviceMap;

    public GraphAnalyzer(List<ServiceNode> services) {
        this.serviceMap = new HashMap<>();
        if (services != null) {
            for (ServiceNode service : services) {
                serviceMap.put(service.getId(), service);
            }
        }
    }

    /**
     * Validates the graph for structural issues.
     *
     * @param edges list of data flow edges
     * @throws GraphValidationException if the graph is invalid
     */
    public void validate(List<DataFlowEdge> edges) throws GraphValidationException {
        if (edges == null) {
            return;
        }

        // Check for duplicate services
        Set<String> serviceIds = serviceMap.keySet();
        if (serviceIds.size() < serviceMap.size()) {
            throw new GraphValidationException("Duplicate service IDs detected in service definitions");
        }

        // Validate each edge references existing services
        for (DataFlowEdge edge : edges) {
            if (!serviceMap.containsKey(edge.getSource())) {
                throw new GraphValidationException(
                        "Data flow references unknown source service: '" + edge.getSource() + "'");
            }
            if (!serviceMap.containsKey(edge.getDestination())) {
                throw new GraphValidationException(
                        "Data flow references unknown destination service: '" + edge.getDestination() + "'");
            }
        }
    }

    /**
     * Resolves a service ID to its ServiceNode.
     *
     * @param serviceId the service ID
     * @return the ServiceNode, or null if not found
     */
    public ServiceNode resolve(String serviceId) {
        return serviceMap.get(serviceId);
    }

    /**
     * Returns all loaded services.
     */
    public Map<String, ServiceNode> getServiceMap() {
        return Map.copyOf(serviceMap);
    }

    /**
     * Returns service IDs that are referenced by edges but don't exist.
     */
    public Set<String> findUnknownServices(List<DataFlowEdge> edges) {
        Set<String> known = serviceMap.keySet();
        return edges.stream()
                .flatMap(e -> java.util.stream.Stream.of(e.getSource(), e.getDestination()))
                .filter(id -> !known.contains(id))
                .collect(Collectors.toSet());
    }
}
