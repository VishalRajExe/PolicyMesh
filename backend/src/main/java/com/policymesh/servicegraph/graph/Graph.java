package com.policymesh.servicegraph.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the service mesh graph containing nodes and edges.
 */
public class Graph {
    private final Map<Long, GraphNode> nodes;
    private final List<GraphEdge> edges;

    public Graph(Map<Long, GraphNode> nodes, List<GraphEdge> edges) {
        this.nodes = nodes != null ? nodes : new HashMap<>();
        this.edges = edges != null ? edges : List.of();
    }

    public Map<Long, GraphNode> getNodes() {
        return nodes;
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }

    public GraphNode getNode(Long nodeId) {
        return nodes.get(nodeId);
    }

    public boolean containsNode(Long nodeId) {
        return nodes.containsKey(nodeId);
    }

    @Override
    public String toString() {
        return "Graph{" +
                "nodes=" + nodes.size() +
                ", edges=" + edges.size() +
                '}';
    }
}