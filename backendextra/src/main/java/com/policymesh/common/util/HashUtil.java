package com.policymesh.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Small SHA-256 helper used by the lineage hash chain.
 * Kept dependency-free (java.security only) for auditability.
 */
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on every standard JVM.
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
