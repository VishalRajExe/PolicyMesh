package com.policymesh.servicegraph;

import com.policymesh.policy.PolicyVocabulary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

public final class GraphDtos {
  private GraphDtos() {}

  /** meshZone is optional per docs/DATABASE_SCHEMA.md. */
  public record ServiceRequest(@NotBlank String name,
                               @NotBlank String region,
                               String meshZone,
                               @NotBlank String environment,
                               String description) {}

  public record ServiceResponse(Long id, String name, String region, String meshZone, String environment,
                                String description, Instant createdAt, Instant updatedAt) {}

  public record EdgeRequest(@NotNull Long sourceServiceId,
                            @NotNull Long destinationServiceId,
                            @NotEmpty Set<@NotBlank String> dataClasses) {}

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
