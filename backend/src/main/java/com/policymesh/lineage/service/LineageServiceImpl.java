package com.policymesh.lineage.service;

import com.policymesh.policy.engine.Decision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

/**
 * Implementation of the LineageService interface.
 * Creates SHA-256 hash-based lineage records for enforcement checks.
 */
@Service
@RequiredArgsConstructor
public class LineageServiceImpl implements LineageService {

    /**
     * Creates a lineage record for an enforcement check.
     * Generates a SHA-256 hash based on the input parameters and timestamp.
     *
     * @param sourceService the source service initiating the data transfer
     * @param destinationService the destination service for the data transfer
     * @param sourceRegion the region of the source service
     * @param destinationRegion the region of the destination service
     * @param dataClass the classification of the data being transferred
     * @param tags additional tags associated with the data
     * @param decision the enforcement decision made
     * @return a unique hash representing the lineage record
     */
    @Override
    public String createEnforcementRecord(
            String sourceService,
            String destinationService,
            String sourceRegion,
            String destinationRegion,
            String dataClass,
            java.util.List<String> tags,
            Decision decision) {

        // Create a string representation of the lineage data
        StringBuilder lineageData = new StringBuilder();
        lineageData.append(sourceService).append("|");
        lineageData.append(destinationService).append("|");
        lineageData.append(sourceRegion).append("|");
        lineageData.append(destinationRegion).append("|");
        lineageData.append(dataClass).append("|");

        // Add tags in a consistent order
        if (tags != null) {
            tags.stream()
                    .sorted()
                    .forEach(tag -> lineageData.append(tag).append(","));
        }
        lineageData.append("|");

        lineageData.append(decision).append("|");
        lineageData.append(Instant.now().toEpochMilli());

        // Generate SHA-256 hash
        return sha256(lineageData.toString());
    }

    /**
     * Generates a SHA-256 hash of the input string.
     *
     * @param input the string to hash
     * @return the SHA-256 hash as a hexadecimal string
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}