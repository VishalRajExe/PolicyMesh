package com.policymesh.ci.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a service in the infrastructure graph.
 * 
 * Maps to the JSON format:
 * <pre>
 * {
 *   "id": "orders-api",
 *   "name": "Orders API",
 *   "region": "EU",
 *   "environment": "production"
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceNode {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("region")
    private String region;

    @JsonProperty("environment")
    private String environment;

    @JsonCreator
    public ServiceNode() {
        // default constructor for deserialization
    }

    public ServiceNode(String id, String name, String region, String environment) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.environment = environment;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    @Override
    public String toString() {
        return String.format("ServiceNode{id='%s', name='%s', region='%s', environment='%s'}",
                id, name, region, environment);
    }
}
