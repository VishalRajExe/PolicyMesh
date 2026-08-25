package com.policymesh.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class GitHubWebhookVerifier {
  private static final Logger log = LoggerFactory.getLogger(GitHubWebhookVerifier.class);
  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final String SIGNATURE_PREFIX = "sha256=";

  private final String webhookSecret;

  public GitHubWebhookVerifier(@Value("${github.webhook.secret:}") String webhookSecret) {
    this.webhookSecret = webhookSecret != null ? webhookSecret.trim() : "";
  }

  /**
   * Verifies the GitHub X-Hub-Signature-256 header against the raw payload bytes.
   *
   * @param signatureHeader value of X-Hub-Signature-256 header (e.g. "sha256=12345...")
   * @param payloadBytes raw UTF-8 body bytes
   * @return true if valid signature, false otherwise
   */
  public boolean verify(String signatureHeader, byte[] payloadBytes) {
    if (signatureHeader == null || signatureHeader.isBlank()) {
      log.warn("GitHub webhook verification failed: missing X-Hub-Signature-256 header");
      return false;
    }

    if (webhookSecret.isEmpty()) {
      log.warn("GitHub webhook verification rejected: GITHUB_WEBHOOK_SECRET is not configured on server.");
      return false;
    }

    String trimmedSignature = signatureHeader.trim();
    if (!trimmedSignature.startsWith(SIGNATURE_PREFIX)) {
      log.warn("GitHub webhook verification failed: invalid signature format (missing 'sha256=' prefix)");
      return false;
    }

    String expectedHex = trimmedSignature.substring(SIGNATURE_PREFIX.length()).trim();
    byte[] expectedBytes;
    try {
      expectedBytes = HexFormat.of().parseHex(expectedHex);
    } catch (IllegalArgumentException e) {
      log.warn("GitHub webhook verification failed: signature is not valid hex");
      return false;
    }

    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
      mac.init(secretKeySpec);
      byte[] computedHash = mac.doFinal(payloadBytes != null ? payloadBytes : new byte[0]);

      // Constant-time comparison to prevent timing attacks
      return MessageDigest.isEqual(computedHash, expectedBytes);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      log.error("Crypto error during webhook HMAC computation: {}", e.getMessage(), e);
      return false;
    }
  }

  /**
   * Utility for testing and local signature generation.
   */
  public static String computeSignature(String secret, byte[] payloadBytes) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
      mac.init(secretKeySpec);
      byte[] hash = mac.doFinal(payloadBytes != null ? payloadBytes : new byte[0]);
      return SIGNATURE_PREFIX + HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new RuntimeException("Failed to compute HMAC signature: " + e.getMessage(), e);
    }
  }
}