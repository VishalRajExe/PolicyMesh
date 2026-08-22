package com.policymesh.ci.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a data-flow edge between two services.
 * 
 * Maps to the JSON format:
 * <pre>
 * {
 *   "source": "orders-api",
 *   "destination": "payments-api",
 *   "dataClasses": ["PII"]
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataFlowEdge {

    @JsonProperty("source")
    private String source;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("dataClasses")
    private List<String> dataClasses;

    @JsonCreator
    public DataFlowEdge() {
        // default constructor for deserialization
    }

    public DataFlowEdge(String source, String destination, List<String> dataClasses) {
        this.source = source;
        this.destination = destination;
        this.dataClasses = dataClasses != null ? List.copyOf(dataClasses) : List.of();
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public List<String> getDataClasses() {
        return dataClasses != null ? List.copyOf(dataClasses) : List.of();
    }
    public void setDataClasses(List<String> dataClasses) {
        this.dataClasses = dataClasses != null ? List.copyOf(dataClasses) : List.of();
    }

    @Override
    public String toString() {
        return String.format("DataFlowEdge{source='%s', destination='%s', dataClasses=%s}",
                source, destination, dataClasses);
    }
}
