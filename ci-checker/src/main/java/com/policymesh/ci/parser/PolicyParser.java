package com.policymesh.ci.parser;

import com.policymesh.ci.model.Policy;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses policy YAML files from a directory.
 *
 * Expected YAML format:
 * <pre>
 * policy:
 *   id: EU-PII-001
 *   name: EU PII Protection
 *   jurisdiction: EU
 *   dataClass: PII
 *   allowedRegions:
 *     - EU
 *   deniedRegions:
 *     - US
 *     - CN
 * </pre>
 *
 * Validation:
 * - policy key must exist
 * - id must exist
 * - dataClass must exist
 * - allowedRegions must exist and be a non-empty list
 */
public class PolicyParser {

    private final Yaml yaml;

    public PolicyParser() {
        this.yaml = new Yaml();
    }

    /**
     * Parses all .yaml/.yml files in the given directory.
     *
     * @param policyDir the directory containing policy files
     * @return list of parsed policies
     * @throws PolicyParseException if any file is malformed or invalid
     */
    @SuppressWarnings("unchecked")
    public List<Policy> parseDirectory(Path policyDir) throws PolicyParseException {
        if (policyDir == null || !Files.isDirectory(policyDir)) {
            throw new PolicyParseException("Policy directory does not exist: " + policyDir);
        }

        List<Policy> policies = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(policyDir, "*.{yaml,yml}")) {
            for (Path file : stream) {
                policies.addAll(parseFile(file));
            }
        } catch (IOException e) {
            throw new PolicyParseException("Failed to read policy directory: " + e.getMessage(), e);
        }

        return policies;
    }

    /**
     * Recursively parses policy files from a directory and all subdirectories.
     */
    public List<Policy> parseDirectoryRecursive(Path policyDir) throws PolicyParseException {
        if (policyDir == null || !Files.isDirectory(policyDir)) {
            throw new PolicyParseException("Policy directory does not exist: " + policyDir);
        }

        List<Policy> policies = new ArrayList<>();
        parseDirectoryRecursiveHelper(policyDir, policies);
        return policies;
    }

    private void parseDirectoryRecursiveHelper(Path dir, List<Policy> policies) throws PolicyParseException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    parseDirectoryRecursiveHelper(entry, policies);
                } else {
                    String fileName = entry.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                        policies.addAll(parseFile(entry));
                    }
                }
            }
        } catch (IOException e) {
            throw new PolicyParseException("Failed to read directory: " + dir + " - " + e.getMessage(), e);
        }
    }

    /**
     * Parses a single policy YAML file.
     *
     * @param filePath path to the YAML file
     * @return list of policies in the file (usually 1, but supports multiple)
     * @throws PolicyParseException if the file is malformed or invalid
     */
    @SuppressWarnings("unchecked")
    public List<Policy> parseFile(Path filePath) throws PolicyParseException {
        if (filePath == null || !Files.exists(filePath)) {
            throw new PolicyParseException("Policy file does not exist: " + filePath);
        }

        String content;
        try {
            content = Files.readString(filePath);
        } catch (IOException e) {
            throw new PolicyParseException("Failed to read policy file: " + filePath + " - " + e.getMessage(), e);
        }

        return parseContent(content, filePath.toString());
    }

    /**
     * Parses policy YAML content from a string.
     */
    @SuppressWarnings("unchecked")
    public List<Policy> parseContent(String content, String sourceName) throws PolicyParseException {
        if (content == null || content.isBlank()) {
            throw new PolicyParseException("Policy file is empty: " + sourceName);
        }

        try {
            Object parsed = yaml.load(content);
            if (parsed == null) {
                throw new PolicyParseException("Policy file is empty or null: " + sourceName);
            }

            List<Policy> policies = new ArrayList<>();

            if (parsed instanceof Map<?, ?> map) {
                policies.add(parseSinglePolicy(map, sourceName));
            } else if (parsed instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        policies.add(parseSinglePolicy(map, sourceName));
                    } else {
                        throw new PolicyParseException(
                                "Invalid policy entry in " + sourceName + ": expected a map");
                    }
                }
            } else {
                throw new PolicyParseException(
                        "Invalid policy format in " + sourceName + ": expected a map or list of maps");
            }

            return policies;

        } catch (YAMLException e) {
            throw new PolicyParseException("YAML syntax error in " + sourceName + ": " + e.getMessage(), e);
        } catch (PolicyParseException e) {
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Policy parseSinglePolicy(Map<?, ?> map, String sourceName) throws PolicyParseException {
        // Check for top-level 'policy' key
        Object policyObj = map.get("policy");
        if (policyObj == null) {
            // Check if the map has required policy fields at the top level (flat format)
            if (map.containsKey("id") || map.containsKey("dataClass")) {
                policyObj = map;
            } else {
                throw new PolicyParseException("Missing 'policy' key in " + sourceName
                        + ": expected a 'policy' key wrapping the policy definition");
            }
        }

        if (!(policyObj instanceof Map<?, ?> policyMap)) {
            throw new PolicyParseException("Invalid policy structure in " + sourceName
                    + ": expected 'policy' key with a map value");
        }

        // Validate required fields
        String id = getStringField(policyMap, "id", sourceName);
        String dataClass = getStringField(policyMap, "dataClass", sourceName);
        List<String> allowedRegions = getStringListField(policyMap, "allowedRegions", sourceName);

        // Optional fields
        String name = getStringFieldOptional(policyMap, "name", id);
        String jurisdiction = getStringFieldOptional(policyMap, "jurisdiction", null);
        List<String> deniedRegions = getStringListFieldOptional(policyMap, "deniedRegions");

        return new Policy(id, name, jurisdiction, dataClass, allowedRegions, deniedRegions);
    }

    private String getStringField(Map<?, ?> map, String field, String sourceName) throws PolicyParseException {
        Object value = map.get(field);
        if (value == null || (value instanceof String s && s.isBlank())) {
            throw new PolicyParseException("Missing required field '" + field + "' in " + sourceName);
        }
        return value.toString();
    }

    private String getStringFieldOptional(Map<?, ?> map, String field, String defaultValue) {
        Object value = map.get(field);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringListField(Map<?, ?> map, String field, String sourceName) throws PolicyParseException {
        Object value = map.get(field);
        if (value == null) {
            throw new PolicyParseException("Missing required field '" + field + "' in " + sourceName);
        }
        if (!(value instanceof List<?> list)) {
            throw new PolicyParseException("Field '" + field + "' must be a list in " + sourceName);
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                throw new PolicyParseException("Null value in '" + field + "' list in " + sourceName);
            }
            result.add(item.toString());
        }
        if (result.isEmpty()) {
            throw new PolicyParseException("Field '" + field + "' must not be empty in " + sourceName);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringListFieldOptional(Map<?, ?> map, String field) {
        Object value = map.get(field);
        if (value == null || !(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(item.toString());
            }
        }
        return result;
    }
}
