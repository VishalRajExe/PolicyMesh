package com.policymesh.ci.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.ci.model.ServiceNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses service definition JSON files.
 *
 * Expected JSON format:
 * <pre>
 * {
 *   "services": [
 *     {
 *       "id": "orders-api",
 *       "name": "Orders API",
 *       "region": "EU",
 *       "environment": "production"
 *     }
 *   ]
 * }
 * </pre>
 */
public class ServiceParser {

    private final ObjectMapper objectMapper;

    public ServiceParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses a services JSON file.
     *
     * @param filePath path to the JSON file
     * @return list of parsed service nodes
     * @throws PolicyParseException if the file is malformed or invalid
     */
    public List<ServiceNode> parseFile(Path filePath) throws PolicyParseException {
        if (filePath == null || !Files.exists(filePath)) {
            throw new PolicyParseException("Services file does not exist: " + filePath);
        }

        String content;
        try {
            content = Files.readString(filePath);
        } catch (IOException e) {
            throw new PolicyParseException("Failed to read services file: " + e.getMessage(), e);
        }

        return parseContent(content);
    }

    /**
     * Parses service definitions from JSON content string.
     */
    public List<ServiceNode> parseContent(String content) throws PolicyParseException {
        if (content == null || content.isBlank()) {
            throw new PolicyParseException("Services file is empty");
        }

        try {
            JsonNode root = objectMapper.readTree(content);

            JsonNode servicesNode = root.get("services");
            if (servicesNode == null || !servicesNode.isArray()) {
                throw new PolicyParseException("Missing or invalid 'services' array in JSON");
            }

            List<ServiceNode> services = new ArrayList<>();
            for (JsonNode node : servicesNode) {
                services.add(parseServiceNode(node));
            }

            return services;

        } catch (PolicyParseException e) {
            throw e;
        } catch (Exception e) {
            throw new PolicyParseException("Failed to parse services JSON: " + e.getMessage(), e);
        }
    }

    private ServiceNode parseServiceNode(JsonNode node) throws PolicyParseException {
        String id = getRequiredString(node, "id");
        String name = getOptionalString(node, "name", id);
        String region = getRequiredString(node, "region");
        String environment = getOptionalString(node, "environment", "production");

        return new ServiceNode(id, name, region, environment);
    }

    private String getRequiredString(JsonNode node, String field) throws PolicyParseException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || (value.isTextual() && value.asText().isBlank())) {
            throw new PolicyParseException("Missing required field '" + field + "' in service definition");
        }
        return value.asText();
    }

    private String getOptionalString(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        return value.asText();
    }
}
