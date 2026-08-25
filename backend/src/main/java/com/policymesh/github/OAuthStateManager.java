package com.policymesh.github;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthStateManager {

  private static final long STATE_TTL_SECONDS = 600; // 10 minutes
  private final SecureRandom secureRandom = new SecureRandom();

  private record StateEntry(Long userId, Instant expiresAt) {}

  private final Map<String, StateEntry> activeStates = new ConcurrentHashMap<>();

  public String generateState(Long userId) {
    cleanupExpired();
    byte[] bytes = new byte[24];
    secureRandom.nextBytes(bytes);
    String state = HexFormat.of().formatHex(bytes);

    activeStates.put(state, new StateEntry(userId, Instant.now().plusSeconds(STATE_TTL_SECONDS)));
    return state;
  }

  public Optional<Long> validateAndConsume(String state) {
    if (state == null || state.isBlank()) {
      return Optional.empty();
    }
    StateEntry entry = activeStates.remove(state.trim());
    if (entry == null) {
      return Optional.empty();
    }
    if (Instant.now().isAfter(entry.expiresAt())) {
      return Optional.empty();
    }
    return Optional.of(entry.userId());
  }

  private void cleanupExpired() {
    Instant now = Instant.now();
    activeStates.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
  }
}