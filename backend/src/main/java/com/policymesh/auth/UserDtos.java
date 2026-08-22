package com.policymesh.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class UserDtos {
  private UserDtos() {}

  public record Response(
      Long id,
      String email,
      Role role,
      String status,
      Instant createdAt
  ) {
    public static Response from(User u) {
      return new Response(u.getId(), u.getEmail(), u.getRole(), u.getStatus(), u.getCreatedAt());
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CreateRequest(
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 128) String password,
      @NotNull Role role,
      String status
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record UpdateRequest(
      Role role,
      String status
  ) {}

  public record RolePermission(
      String role,
      String title,
      String description,
      List<String> permissions
  ) {}
}
