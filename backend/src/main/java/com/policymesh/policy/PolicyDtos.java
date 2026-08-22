package com.policymesh.policy;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Set;

public final class PolicyDtos {
  private PolicyDtos() {}

  /** status is optional: absent means DRAFT on create and "unchanged" on update. */
  public record Request(@NotBlank String policyCode,
                        @NotBlank String name,
                        @NotBlank String jurisdiction,
                        @NotBlank String dataClass,
                        Set<String> allowedRegions,
                        Set<String> deniedRegions,
                        PolicyStatus status) {}

  public record YamlRequest(@NotBlank String yaml) {}

  public record Response(Long id, String policyCode, String name, String jurisdiction, String dataClass,
                         Set<String> allowedRegions, Set<String> deniedRegions, PolicyStatus status,
                         int version, Instant createdAt, Instant updatedAt) {}

  public static Response from(Policy p) {
    return new Response(p.getId(), p.getPolicyCode(), p.getName(), p.getJurisdiction(), p.getDataClass(),
        Set.copyOf(p.getAllowedRegions()), Set.copyOf(p.getDeniedRegions()), p.getStatus(),
        p.getVersion(), p.getCreatedAt(), p.getUpdatedAt());
  }
}
