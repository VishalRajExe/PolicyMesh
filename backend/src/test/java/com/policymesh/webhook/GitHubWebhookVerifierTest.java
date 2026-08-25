package com.policymesh.webhook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubWebhookVerifierTest {

  private static final String TEST_SECRET = "test-webhook-secret-key-12345";
  private final GitHubWebhookVerifier verifier = new GitHubWebhookVerifier(TEST_SECRET);

  @Test
  void verifiesValidSignature() {
    byte[] payload = "{\"ref\":\"refs/heads/main\",\"after\":\"abc12345\"}".getBytes(StandardCharsets.UTF_8);
    String signature = GitHubWebhookVerifier.computeSignature(TEST_SECRET, payload);

    assertThat(verifier.verify(signature, payload)).isTrue();
  }

  @Test
  void rejectsTamperedPayload() {
    byte[] originalPayload = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);
    String signature = GitHubWebhookVerifier.computeSignature(TEST_SECRET, originalPayload);

    byte[] tamperedPayload = "{\"ref\":\"refs/heads/malicious\"}".getBytes(StandardCharsets.UTF_8);
    assertThat(verifier.verify(signature, tamperedPayload)).isFalse();
  }

  @Test
  void rejectsMissingOrInvalidSignatureHeader() {
    byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

    assertThat(verifier.verify(null, payload)).isFalse();
    assertThat(verifier.verify("", payload)).isFalse();
    assertThat(verifier.verify("sha256=invalid-non-hex", payload)).isFalse();
    assertThat(verifier.verify("md5=12345", payload)).isFalse();
  }

  @Test
  void rejectsWhenServerSecretNotConfigured() {
    GitHubWebhookVerifier emptySecretVerifier = new GitHubWebhookVerifier("");
    byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
    String signature = GitHubWebhookVerifier.computeSignature("any-secret", payload);

    assertThat(emptySecretVerifier.verify(signature, payload)).isFalse();
  }
}