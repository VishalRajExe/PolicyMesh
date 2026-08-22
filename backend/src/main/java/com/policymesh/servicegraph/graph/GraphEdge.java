package com.policymesh.servicegraph.graph;

import com.policymesh.servicegraph.entity.DataFlowEdge;
import java.util.List;

/**
 * Represents an edge in the service mesh graph.
 * Contains the essential information needed for graph analysis.
 */
public class GraphEdge {
    private final Long id;
    private final Long sourceServiceId;
    private final Long destinationServiceId;
    private final List<String> dataClasses;

    public GraphEdge(DataFlowEdge dataFlowEdge) {
        this.id = dataFlowEdge.getId();
        this.sourceServiceId = dataFlowEdge.getSourceServiceId();
        this.destinationServiceId = dataFlowEdge.getDestinationServiceId();
        this.dataClasses = dataFlowEdge.getDataClasses();
    }

    public Long getId() {
        return id;
    }

    public Long getSourceServiceId() {
        return sourceServiceId;
    }

    public Long getDestinationServiceId() {
        return destinationServiceId;
    }

    public List<String> getDataClasses() {
        return dataClasses;
    }

    @Override
    public String toString() {
        return "GraphEdge{" +
                "id=" + id +
                ", sourceServiceId=" + sourceServiceId +
                ", destinationServiceId=" + destinationServiceId +
                ", dataClasses=" + dataClasses +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphEdge graphEdge = (GraphEdge) o;
        return id.equals(graphEdge.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}