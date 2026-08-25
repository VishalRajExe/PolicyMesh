package com.policymesh.github;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthStateManagerTest {

  private final OAuthStateManager stateManager = new OAuthStateManager();

  @Test
  void generatesAndConsumesStateForUser() {
    Long userId = 42L;
    String state = stateManager.generateState(userId);

    assertThat(state).isNotBlank();
    assertThat(state).hasSize(48); // 24 bytes hex

    Optional<Long> consumedUserId = stateManager.validateAndConsume(state);
    assertThat(consumedUserId).isPresent().contains(userId);

    // Replay attempt must fail
    Optional<Long> replayAttempt = stateManager.validateAndConsume(state);
    assertThat(replayAttempt).isEmpty();
  }

  @Test
  void rejectsUnknownOrInvalidState() {
    assertThat(stateManager.validateAndConsume("non-existent-state")).isEmpty();
    assertThat(stateManager.validateAndConsume(null)).isEmpty();
    assertThat(stateManager.validateAndConsume("")).isEmpty();
  }
}