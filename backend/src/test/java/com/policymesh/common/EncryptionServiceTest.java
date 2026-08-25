package com.policymesh.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

  private final EncryptionService encryptionService = new EncryptionService("test-secret-key-32-chars-long-for-testing-12345");

  @Test
  void encryptsAndDecryptsTokenSuccessfully() {
    String token = "gho_test_oauth_access_token_1234567890abcdef";
    String encrypted = encryptionService.encrypt(token);

    assertThat(encrypted).isNotNull();
    assertThat(encrypted).isNotEqualTo(token);

    String decrypted = encryptionService.decrypt(encrypted);
    assertThat(decrypted).isEqualTo(token);
  }

  @Test
  void generatesDistinctCiphertextForSamePlaintextDueToRandomIV() {
    String token = "gho_static_token_value_xyz";
    String cipher1 = encryptionService.encrypt(token);
    String cipher2 = encryptionService.encrypt(token);

    assertThat(cipher1).isNotEqualTo(cipher2);
    assertThat(encryptionService.decrypt(cipher1)).isEqualTo(token);
    assertThat(encryptionService.decrypt(cipher2)).isEqualTo(token);
  }

  @Test
  void rejectsTamperedCiphertext() {
    String token = "gho_sensitive_token";
    String encrypted = encryptionService.encrypt(token);

    // Tamper with payload in the middle of ciphertext
    char[] chars = encrypted.toCharArray();
    int idx = chars.length / 2;
    chars[idx] = (chars[idx] == 'A') ? 'B' : 'A';
    String tampered = new String(chars);

    assertThatThrownBy(() -> encryptionService.decrypt(tampered))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void handlesEmptyAndNullSafely() {
    assertThat(encryptionService.encrypt(null)).isNull();
    assertThat(encryptionService.encrypt("")).isEmpty();
    assertThat(encryptionService.decrypt(null)).isNull();
    assertThat(encryptionService.decrypt("")).isEmpty();
  }
}