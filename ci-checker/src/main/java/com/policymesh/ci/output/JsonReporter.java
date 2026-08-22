package com.policymesh.ci.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.policymesh.ci.model.ComplianceResult;

import java.io.IOException;

/**
 * Formats compliance results as machine-readable JSON.
 *
 * When using JSON output, diagnostic logs go to stderr.
 * stdout receives only the JSON result.
 */
public class JsonReporter {

    private final ObjectMapper objectMapper;

    public JsonReporter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Prints the compliance result as JSON to stdout.
     */
    public void report(ComplianceResult result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            System.out.println(json);
        } catch (IOException e) {
            System.err.println("Error serializing result to JSON: " + e.getMessage());
        }
    }

    /**
     * Logs a message to stderr (for JSON output mode).
     */
    public void logToStderr(String message) {
        System.err.println("[PolicyMesh CI] " + message);
    }
}
