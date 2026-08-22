package com.policymesh.ci.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing operations.
 * Used for change detection and content fingerprinting.
 */
public final class HashUtil {

    private HashUtil() {
        // utility class
    }

    /**
     * Computes SHA-256 hash of the given string.
     */
    public static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in standard JVM
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Computes a short hash (first 8 characters of SHA-256).
     */
    public static String shortHash(String content) {
        return sha256(content).substring(0, 8);
    }
}
