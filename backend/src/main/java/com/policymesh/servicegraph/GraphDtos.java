package com.policymesh.servicegraph;

import com.policymesh.policy.PolicyVocabulary;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class GraphDtos {
  private GraphDtos() {}

  /** meshZone is optional per docs/DATABASE_SCHEMA.md. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ServiceRequest(
      @NotBlank @Size(min = 2, max = 64)
      @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_-]*$", message = "Service name must be alphanumeric with optional hyphens or underscores")
      String name,

      @NotBlank @Size(min = 2, max = 50)
      @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Region must be alphanumeric with optional hyphens or underscores")
      String region,

      @Size(max = 100)
      @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "meshZone must be alphanumeric with optional hyphens or underscores")
      String meshZone,

      @NotBlank @Size(min = 2, max = 50)
      @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "environment must be alphanumeric")
      String environment,

      @Size(max = 1000)
      String description) {}

  public record ServiceResponse(Long id, String name, String region, String meshZone, String environment,
                                String description, Instant createdAt, Instant updatedAt) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record EdgeRequest(
      @NotNull @Positive Long sourceServiceId,
      @NotNull @Positive Long destinationServiceId,
      @NotEmpty @Size(max = 20) Set<@NotBlank @Size(max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String> dataClasses) {}

  public record EdgeResponse(Long id, Long sourceServiceId, Long destinationServiceId,
                             Set<String> dataClasses, Instant createdAt, Instant updatedAt) {}

  static ServiceResponse service(ServiceNode s) {
    return new ServiceResponse(s.getId(), s.getName(), s.getRegion(), s.getMeshZone(), s.getEnvironment(),
        s.getDescription(), s.getCreatedAt(), s.getUpdatedAt());
  }

  static EdgeResponse edge(DataFlowEdge e) {
    return new EdgeResponse(e.getId(), e.getSourceServiceId(), e.getDestinationServiceId(),
        Set.copyOf(e.getDataClasses()), e.getCreatedAt(), e.getUpdatedAt());
  }

  static TreeSet<String> canonicalClasses(Set<String> dataClasses) {
    TreeSet<String> result = new TreeSet<>();
    if (dataClasses != null) {
      dataClasses.stream().filter(x -> x != null && !x.isBlank())
          .map(PolicyVocabulary::canonicalDataClass)
          .forEach(result::add);
    }
    return result;
  }
}
