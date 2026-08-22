package com.policymesh.ci.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymesh.ci.model.DataFlowEdge;
import com.policymesh.ci.model.Policy;
import com.policymesh.ci.model.ServiceNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for the PolicyMesh Spring Boot backend.
 *
 * Used in "Backend-assisted mode" when policies/services/data-flows
 * are fetched from the central API instead of local files.
 *
 * Endpoints used:
 *   GET /api/v1/policies
 *   GET /api/v1/services
 *   GET /api/v1/graph
 *
 * Authentication:
 *   Authorization: Bearer <token>
 *
 * The token is never logged.
 */
public class PolicyMeshBackendClient {

    private final String baseUrl;
    private final String token;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PolicyMeshBackendClient(String baseUrl, String token) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches policies from the backend.
     *
     * @return list of policies
     * @throws BackendAccessException if the request fails
     */
    public List<Policy> fetchPolicies() throws BackendAccessException {
        try {
            JsonNode root = get("/api/v1/policies");
            List<Policy> policies = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode node : root) {
                    policies.add(objectMapper.treeToValue(node, Policy.class));
                }
            } else if (root.has("policies")) {
                for (JsonNode node : root.get("policies")) {
                    policies.add(objectMapper.treeToValue(node, Policy.class));
                }
            }

            return policies;
        } catch (BackendAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new BackendAccessException("Failed to parse policies from backend: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches services from the backend.
     */
    public List<ServiceNode> fetchServices() throws BackendAccessException {
        try {
            JsonNode root = get("/api/v1/services");
            List<ServiceNode> services = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode node : root) {
                    services.add(objectMapper.treeToValue(node, ServiceNode.class));
                }
            } else if (root.has("services")) {
                for (JsonNode node : root.get("services")) {
                    services.add(objectMapper.treeToValue(node, ServiceNode.class));
                }
            }

            return services;
        } catch (BackendAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new BackendAccessException("Failed to parse services from backend: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches data-flow edges from the backend graph endpoint.
     */
    public List<DataFlowEdge> fetchDataFlows() throws BackendAccessException {
        try {
            JsonNode root = get("/api/v1/graph");
            List<DataFlowEdge> edges = new ArrayList<>();

            if (root.has("dataFlows") && root.get("dataFlows").isArray()) {
                for (JsonNode node : root.get("dataFlows")) {
                    edges.add(objectMapper.treeToValue(node, DataFlowEdge.class));
                }
            } else if (root.has("edges") && root.get("edges").isArray()) {
                for (JsonNode node : root.get("edges")) {
                    edges.add(objectMapper.treeToValue(node, DataFlowEdge.class));
                }
            } else if (root.isArray()) {
                for (JsonNode node : root) {
                    edges.add(objectMapper.treeToValue(node, DataFlowEdge.class));
                }
            }

            return edges;
        } catch (BackendAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new BackendAccessException("Failed to parse data flows from backend: " + e.getMessage(), e);
        }
    }

    /**
     * Performs a health check against the backend.
     */
    public boolean isHealthy() {
        try {
            get("/actuator/health");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Makes an authenticated GET request to the backend.
     */
    private JsonNode get(String path) throws BackendAccessException {
        String url = baseUrl + path;

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30));

        if (token != null && !token.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            switch (response.statusCode()) {
                case 200:
                    return objectMapper.readTree(response.body());
                case 401:
                    throw new BackendAccessException("Authentication failed (401). Check your API token.");
                case 403:
                    throw new BackendAccessException("Access denied (403). Your token may lack required permissions.");
                case 404:
                    throw new BackendAccessException("Endpoint not found (404): " + url);
                case 500:
                    throw new BackendAccessException("Backend internal error (500): " + response.body());
                default:
                    throw new BackendAccessException(
                            "Unexpected response from backend: HTTP " + response.statusCode());
            }
        } catch (BackendAccessException e) {
            throw e;
        } catch (IOException e) {
            throw new BackendAccessException("Connection failed: " + e.getMessage() + " [" + url + "]", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackendAccessException("Request interrupted: " + e.getMessage(), e);
        }
    }
}
