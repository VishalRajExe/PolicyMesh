package com.policymesh.servicegraph.graph;

import com.policymesh.servicegraph.entity.ServiceNode;

/**
 * Represents a node in the service mesh graph.
 * Contains the essential information needed for graph analysis.
 */
public class GraphNode {
    private final Long id;
    private final String name;
    private final String region;
    private final String meshZone;
    private final String environment;
    private final String description;

    public GraphNode(ServiceNode serviceNode) {
        this.id = serviceNode.getId();
        this.name = serviceNode.getName();
        this.region = serviceNode.getRegion();
        this.meshZone = serviceNode.getMeshZone();
        this.environment = serviceNode.getEnvironment();
        this.description = serviceNode.getDescription();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public String getMeshZone() {
        return meshZone;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "GraphNode{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", region='" + region + '\'' +
                ", meshZone='" + meshZone + '\'' +
                ", environment='" + environment + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return id.equals(graphNode.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}