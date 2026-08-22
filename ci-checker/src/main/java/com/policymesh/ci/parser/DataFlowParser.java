package com.policymesh.ci.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.ci.model.DataFlowEdge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses data-flow definition JSON files.
 *
 * Expected JSON format:
 * <pre>
 * {
 *   "dataFlows": [
 *     {
 *       "source": "orders-api",
 *       "destination": "payments-api",
 *       "dataClasses": ["PII"]
 *     }
 *   ]
 * }
 * </pre>
 */
public class DataFlowParser {

    private final ObjectMapper objectMapper;

    public DataFlowParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses a data-flows JSON file.
     *
     * @param filePath path to the JSON file
     * @return list of parsed data-flow edges
     * @throws PolicyParseException if the file is malformed or invalid
     */
    public List<DataFlowEdge> parseFile(Path filePath) throws PolicyParseException {
        if (filePath == null || !Files.exists(filePath)) {
            throw new PolicyParseException("Data flows file does not exist: " + filePath);
        }

        String content;
        try {
            content = Files.readString(filePath);
        } catch (IOException e) {
            throw new PolicyParseException("Failed to read data flows file: " + e.getMessage(), e);
        }

        return parseContent(content);
    }

    /**
     * Parses data-flow definitions from JSON content string.
     */
    public List<DataFlowEdge> parseContent(String content) throws PolicyParseException {
        if (content == null || content.isBlank()) {
            throw new PolicyParseException("Data flows file is empty");
        }

        try {
            JsonNode root = objectMapper.readTree(content);

            JsonNode flowsNode = root.get("dataFlows");
            if (flowsNode == null || !flowsNode.isArray()) {
                throw new PolicyParseException("Missing or invalid 'dataFlows' array in JSON");
            }

            List<DataFlowEdge> edges = new ArrayList<>();
            for (JsonNode node : flowsNode) {
                edges.add(parseDataFlowEdge(node));
            }

            return edges;

        } catch (PolicyParseException e) {
            throw e;
        } catch (Exception e) {
            throw new PolicyParseException("Failed to parse data flows JSON: " + e.getMessage(), e);
        }
    }

    private DataFlowEdge parseDataFlowEdge(JsonNode node) throws PolicyParseException {
        String source = getRequiredString(node, "source");
        String destination = getRequiredString(node, "destination");
        List<String> dataClasses = getRequiredStringList(node, "dataClasses");

        return new DataFlowEdge(source, destination, dataClasses);
    }

    private String getRequiredString(JsonNode node, String field) throws PolicyParseException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || (value.isTextual() && value.asText().isBlank())) {
            throw new PolicyParseException("Missing required field '" + field + "' in data flow definition");
        }
        return value.asText();
    }

    private List<String> getRequiredStringList(JsonNode node, String field) throws PolicyParseException {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            throw new PolicyParseException("Missing or invalid '" + field + "' array in data flow definition");
        }

        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (item == null || item.isNull()) {
                throw new PolicyParseException("Null value in '" + field + "' array");
            }
            result.add(item.asText());
        }

        if (result.isEmpty()) {
            throw new PolicyParseException("'" + field + "' must not be empty");
        }

        return result;
    }
}
