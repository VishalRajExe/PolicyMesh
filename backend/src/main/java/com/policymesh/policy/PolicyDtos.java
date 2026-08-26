package com.policymesh.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class PolicyDtos {
  private PolicyDtos() {}

  /** status is optional: absent means DRAFT on create and "unchanged" on update. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Request(
      @NotBlank @Size(min = 1, max = 100)
      @Pattern(regexp = "^[A-Z0-9][A-Z0-9_-]*$", message = "policyCode must be alphanumeric with optional hyphens or underscores")
      String policyCode,

      @NotBlank @Size(min = 1, max = 255)
      String name,

      @NotBlank @Size(min = 1, max = 50)
      @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "jurisdiction must be alphanumeric with hyphens or underscores")
      String jurisdiction,

      @NotBlank @Size(min = 1, max = 50)
      @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "dataClass must be alphanumeric with hyphens or underscores")
      String dataClass,

      Set<@NotBlank @Size(max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String> allowedRegions,
      Set<@NotBlank @Size(max = 50) @Pattern(regexp = "^[a-zA-Z0-9_-]+$") String> deniedRegions,
      PolicyStatus status) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record YamlRequest(@NotBlank @Size(max = 1048576, message = "YAML content must not exceed 1MB") String yaml) {}

  public record Response(Long id, String policyCode, String name, String jurisdiction, String dataClass,
                         Set<String> allowedRegions, Set<String> deniedRegions, PolicyStatus status,
                         int version, Instant createdAt, Instant updatedAt) {}

  public static Response from(Policy p) {
    Set<String> allowed = p.getAllowedRegions() != null ? Set.copyOf(p.getAllowedRegions()) : Set.of();
    Set<String> denied = p.getDeniedRegions() != null ? Set.copyOf(p.getDeniedRegions()) : Set.of();
    return new Response(p.getId(), p.getPolicyCode(), p.getName(), p.getJurisdiction(), p.getDataClass(),
        allowed, denied, p.getStatus(), p.getVersion(), p.getCreatedAt(), p.getUpdatedAt());
  }
}
