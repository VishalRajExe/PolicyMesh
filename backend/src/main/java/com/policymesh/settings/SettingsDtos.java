package com.policymesh.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

public final class SettingsDtos {
  private SettingsDtos() {}

  public record ProfileResponse(
      Long id,
      String email,
      String role,
      String status,
      Instant createdAt
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ChangePasswordRequest(
      @NotBlank String currentPassword,
      @NotBlank @Size(min = 8, max = 128) String newPassword
  ) {}

  public record SystemSettings(
      Instant timestamp,
      ComponentStatus api,
      ComponentStatus database,
      ComponentStatus redis,
      ComponentStatus kafka,
      ComponentStatus aiService,
      Map<String, Object> governanceEngine
  ) {}

  public record ComponentStatus(
      String name,
      String status,
      String type,
      String details
  ) {}
}
