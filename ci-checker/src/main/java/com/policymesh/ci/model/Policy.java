package com.policymesh.ci.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a data-residency or data-classification policy.
 * 
 * Maps to the YAML format:
 * <pre>
 * policy:
 *   id: EU-PII-001
 *   name: EU PII Protection
 *   jurisdiction: EU
 *   dataClass: PII
 *   allowedRegions:
 *     - EU
 *   deniedRegions:
 *     - US
 *     - CN
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Policy {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonProperty("dataClass")
    private String dataClass;

    @JsonProperty("allowedRegions")
    private java.util.List<String> allowedRegions;

    @JsonProperty("deniedRegions")
    private java.util.List<String> deniedRegions;

    @JsonCreator
    public Policy() {
        // default constructor for deserialization
    }

    public Policy(String id, String name, String jurisdiction, String dataClass,
                  java.util.List<String> allowedRegions, java.util.List<String> deniedRegions) {
        this.id = id;
        this.name = name;
        this.jurisdiction = jurisdiction;
        this.dataClass = dataClass;
        this.allowedRegions = allowedRegions != null ? java.util.List.copyOf(allowedRegions) : java.util.List.of();
        this.deniedRegions = deniedRegions != null ? java.util.List.copyOf(deniedRegions) : java.util.List.of();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

    public String getDataClass() { return dataClass; }
    public void setDataClass(String dataClass) { this.dataClass = dataClass; }

    public java.util.List<String> getAllowedRegions() {
        return allowedRegions != null ? java.util.List.copyOf(allowedRegions) : java.util.List.of();
    }
    public void setAllowedRegions(java.util.List<String> allowedRegions) {
        this.allowedRegions = allowedRegions != null ? java.util.List.copyOf(allowedRegions) : java.util.List.of();
    }

    public java.util.List<String> getDeniedRegions() {
        return deniedRegions != null ? java.util.List.copyOf(deniedRegions) : java.util.List.of();
    }
    public void setDeniedRegions(java.util.List<String> deniedRegions) {
        this.deniedRegions = deniedRegions != null ? java.util.List.copyOf(deniedRegions) : java.util.List.of();
    }

    /**
     * Returns true if this policy applies to the given data class.
     */
    public boolean appliesToDataClass(String dataClass) {
        if (this.dataClass == null || dataClass == null) {
            return false;
        }
        return this.dataClass.equalsIgnoreCase(dataClass);
    }

    @Override
    public String toString() {
        return String.format("Policy{id='%s', name='%s', jurisdiction='%s', dataClass='%s', allowed=%s, denied=%s}",
                id, name, jurisdiction, dataClass, allowedRegions, deniedRegions);
    }
}
