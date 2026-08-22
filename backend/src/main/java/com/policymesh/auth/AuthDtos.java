package com.policymesh.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
  private AuthDtos() {}

  /** Role is optional and defaults to ENGINEER; any of the four documented roles may be requested. */
  public record Register(@Email @NotBlank String email,
                         @Size(min = 8, max = 128) String password,
                         Role role) {}

  public record Login(@Email @NotBlank String email, @NotBlank String password) {}

  public record Registered(Long id, String email, String role) {}

  public record Token(String token, long expiresIn, String tokenType, String role) {}
}
