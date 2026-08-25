package com.policymesh.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  private final SecretKey secretKey;
  private final SecureRandom secureRandom = new SecureRandom();

  public EncryptionService(@Value("${app.jwt-secret:default-secret-key-32-chars-long-abc}") String secret) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      byte[] keyBytes = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
      this.secretKey = new SecretKeySpec(keyBytes, "AES");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to initialize EncryptionService key derivation", e);
    }
  }

  /**
   * Encrypts plaintext string using AES-GCM-256.
   * Produces Base64-encoded [IV (12 bytes) + Ciphertext + Tag (16 bytes)].
   */
  public String encrypt(String plaintext) {
    if (plaintext == null || plaintext.isEmpty()) {
      return plaintext;
    }
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

      byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
      byteBuffer.put(iv);
      byteBuffer.put(cipherText);

      return Base64.getEncoder().encodeToString(byteBuffer.array());
    } catch (Exception e) {
      throw new RuntimeException("Encryption failure: " + e.getMessage(), e);
    }
  }

  /**
   * Decrypts Base64-encoded [IV + Ciphertext + Tag] string using AES-GCM-256.
   */
  public String decrypt(String encryptedBase64) {
    if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
      return encryptedBase64;
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(encryptedBase64);
      if (decoded.length < GCM_IV_LENGTH) {
        throw new IllegalArgumentException("Invalid encrypted payload length");
      }

      ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
      byte[] iv = new byte[GCM_IV_LENGTH];
      byteBuffer.get(iv);

      byte[] cipherText = new byte[byteBuffer.remaining()];
      byteBuffer.get(cipherText);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

      byte[] plainText = cipher.doFinal(cipherText);
      return new String(plainText, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Decryption failure: " + e.getMessage(), e);
    }
  }
}