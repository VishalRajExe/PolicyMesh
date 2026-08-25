package com.policymesh.auth;

import com.policymesh.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private static final int MAX_FAILED_ATTEMPTS = 10;
  private static final long WINDOW_MS = 15 * 60 * 1000;

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final Map<String, Window> loginAttempts = new ConcurrentHashMap<>();

  public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthDtos.Registered register(@Valid @RequestBody AuthDtos.Register request) {
    String email = request.email().trim().toLowerCase();
    if (users.findByEmailIgnoreCase(email).isPresent()) {
      throw ApiException.conflict("An account with that email already exists");
    }
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(encoder.encode(request.password()));
    user.setRole(request.role() != null ? request.role() : Role.ENGINEER);
    user.setName(request.name());
    user.setStatus("ACTIVE");
    user.setEnabled(true);
    user = users.save(user);
    return new AuthDtos.Registered(user.getId(), user.getEmail(), user.getRole().name());
  }

  @PostMapping("/login")
  public AuthDtos.Token login(@Valid @RequestBody AuthDtos.Login request) {
    String email = request.email().trim().toLowerCase();
    Window window = loginAttempts.compute(email, (k, w) -> w == null || w.isExpired() ? new Window() : w);
    if (window.failures >= MAX_FAILED_ATTEMPTS) {
      throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "rate-limit", "Too many failed login attempts; try again later");
    }
    User user = users.findByEmailIgnoreCase(email)
        .orElseThrow(() -> invalidLogin(window));
    if (!encoder.matches(request.password(), user.getPasswordHash())) {
      throw invalidLogin(window);
    }
    loginAttempts.remove(email);
    return new AuthDtos.Token(jwt.issue(user), jwt.expirationSeconds(), "Bearer", user.getRole().name());
  }

  private ApiException invalidLogin(Window window) {
    window.failures++;
    window.refresh();
    return ApiException.unauthorized("Invalid email or password");
  }

  private static final class Window {
    private int failures;
    private long expiresAt = System.currentTimeMillis() + WINDOW_MS;
    boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    void refresh() { expiresAt = System.currentTimeMillis() + WINDOW_MS; }
  }
}
