package com.policymesh.ci;

import com.policymesh.ci.model.DataFlowEdge;
import com.policymesh.ci.model.Policy;
import com.policymesh.ci.model.ServiceNode;
import com.policymesh.ci.parser.DataFlowParser;
import com.policymesh.ci.parser.PolicyParseException;
import com.policymesh.ci.parser.PolicyParser;
import com.policymesh.ci.parser.ServiceParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private final PolicyParser policyParser = new PolicyParser();
    private final ServiceParser serviceParser = new ServiceParser();
    private final DataFlowParser dataFlowParser = new DataFlowParser();

    // --- Policy Parser Tests ---

    @Test
    void validYaml_shouldParse() throws PolicyParseException {
        String yaml = """
                policy:
                  id: EU-PII-001
                  name: EU PII Protection
                  jurisdiction: EU
                  dataClass: PII
                  allowedRegions:
                    - EU
                    - UK
                  deniedRegions:
                    - US
                """;

        List<Policy> policies = policyParser.parseContent(yaml, "test.yaml");
        assertEquals(1, policies.size());

        Policy policy = policies.get(0);
        assertEquals("EU-PII-001", policy.getId());
        assertEquals("EU PII Protection", policy.getName());
        assertEquals("EU", policy.getJurisdiction());
        assertEquals("PII", policy.getDataClass());
        assertEquals(List.of("EU", "UK"), policy.getAllowedRegions());
        assertEquals(List.of("US"), policy.getDeniedRegions());
    }

    @Test
    void yamlMissingId_shouldFail() {
        String yaml = """
                policy:
                  name: EU PII Protection
                  jurisdiction: EU
                  dataClass: PII
                  allowedRegions:
                    - EU
                """;

        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> policyParser.parseContent(yaml, "test.yaml"));
        assertTrue(ex.getMessage().contains("id"));
    }

    @Test
    void yamlMissingDataClass_shouldFail() {
        String yaml = """
                policy:
                  id: EU-PII-001
                  name: EU PII Protection
                  jurisdiction: EU
                  allowedRegions:
                    - EU
                """;

        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> policyParser.parseContent(yaml, "test.yaml"));
        assertTrue(ex.getMessage().contains("dataClass"));
    }

    @Test
    void yamlMissingAllowedRegions_shouldFail() {
        String yaml = """
                policy:
                  id: EU-PII-001
                  dataClass: PII
                  jurisdiction: EU
                """;

        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> policyParser.parseContent(yaml, "test.yaml"));
        assertTrue(ex.getMessage().contains("allowedRegions"));
    }

    @Test
    void yamlEmptyContent_shouldFail() {
        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> policyParser.parseContent("", "test.yaml"));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void yamlMalformedSyntax_shouldFail() {
        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> policyParser.parseContent("{{{{invalid yaml", "test.yaml"));
        assertNotNull(ex.getMessage());
    }

    @Test
    void yamlMissingPolicyKey_shouldFail() {
        String yaml = """
                notPolicy:
                  id: EU-PII-001
                """;

        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> policyParser.parseContent(yaml, "test.yaml"));
        assertTrue(ex.getMessage().contains("policy"));
    }

    @Test
    void yamlFlatFormat_shouldParse() throws PolicyParseException {
        // Also support flat format without 'policy' wrapper
        String yaml = """
                id: EU-PII-001
                name: EU PII Protection
                jurisdiction: EU
                dataClass: PII
                allowedRegions:
                  - EU
                """;

        List<Policy> policies = policyParser.parseContent(yaml, "test.yaml");
        assertEquals(1, policies.size());
        assertEquals("EU-PII-001", policies.get(0).getId());
    }

    @Test
    void nonExistentPolicyDir_shouldFail() {
        assertThrows(PolicyParseException.class,
                () -> policyParser.parseDirectory(Path.of("/nonexistent/path")));
    }

    // --- Service Parser Tests ---

    @Test
    void validServicesJson_shouldParse() throws PolicyParseException {
        String json = """
                {
                  "services": [
                    {
                      "id": "orders-api",
                      "name": "Orders API",
                      "region": "EU",
                      "environment": "production"
                    }
                  ]
                }
                """;

        List<ServiceNode> services = serviceParser.parseContent(json);
        assertEquals(1, services.size());

        ServiceNode service = services.get(0);
        assertEquals("orders-api", service.getId());
        assertEquals("Orders API", service.getName());
        assertEquals("EU", service.getRegion());
        assertEquals("production", service.getEnvironment());
    }

    @Test
    void servicesJsonMissingId_shouldFail() {
        String json = """
                {
                  "services": [
                    {
                      "name": "Orders API",
                      "region": "EU"
                    }
                  ]
                }
                """;

        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> serviceParser.parseContent(json));
        assertTrue(ex.getMessage().contains("id"));
    }

    @Test
    void servicesJsonMissingRegion_shouldFail() {
        String json = """
                {
                  "services": [
                    {
                      "id": "orders-api",
                      "name": "Orders API"
                    }
                  ]
                }
                """;

        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> serviceParser.parseContent(json));
        assertTrue(ex.getMessage().contains("region"));
    }

    @Test
    void servicesJsonEmptyContent_shouldFail() {
        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> serviceParser.parseContent(""));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void servicesJsonMissingServicesKey_shouldFail() {
        String json = """
                {
                  "notServices": []
                }
                """;

        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> serviceParser.parseContent(json));
        assertTrue(ex.getMessage().contains("services"));
    }

    @Test
    void nonExistentServicesFile_shouldFail() {
        assertThrows(PolicyParseException.class,
                () -> serviceParser.parseFile(Path.of("/nonexistent/services.json")));
    }

    // --- DataFlow Parser Tests ---

    @Test
    void validDataFlowsJson_shouldParse() throws PolicyParseException {
        String json = """
                {
                  "dataFlows": [
                    {
                      "source": "orders-api",
                      "destination": "payments-api",
                      "dataClasses": ["PII"]
                    }
                  ]
                }
                """;

        List<DataFlowEdge> edges = dataFlowParser.parseContent(json);
        assertEquals(1, edges.size());

        DataFlowEdge edge = edges.get(0);
        assertEquals("orders-api", edge.getSource());
        assertEquals("payments-api", edge.getDestination());
        assertEquals(List.of("PII"), edge.getDataClasses());
    }

    @Test
    void dataFlowsJsonMissingSource_shouldFail() {
        String json = """
                {
                  "dataFlows": [
                    {
                      "destination": "payments-api",
                      "dataClasses": ["PII"]
                    }
                  ]
                }
                """;

        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> dataFlowParser.parseContent(json));
        assertTrue(ex.getMessage().contains("source"));
    }

    @Test
    void dataFlowsJsonMissingDataClasses_shouldFail() {
        String json = """
                {
                  "dataFlows": [
                    {
                      "source": "orders-api",
                      "destination": "payments-api"
                    }
                  ]
                }
                """;

        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> dataFlowParser.parseContent(json));
        assertTrue(ex.getMessage().contains("dataClasses"));
    }

    @Test
    void dataFlowsJsonEmptyDataClasses_shouldFail() {
        String json = """
                {
                  "dataFlows": [
                    {
                      "source": "orders-api",
                      "destination": "payments-api",
                      "dataClasses": []
                    }
                  ]
                }
                """;

        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> dataFlowParser.parseContent(json));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void dataFlowsJsonEmptyContent_shouldFail() {
        PolicyParseException ex = assertThrows(PolicyParseException.class,
                () -> dataFlowParser.parseContent(""));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void nonExistentDataFlowsFile_shouldFail() {
        assertThrows(PolicyParseException.class,
                () -> dataFlowParser.parseFile(Path.of("/nonexistent/dataflows.json")));
    }

    private Path resolveExamplePath(String relative) {
        Path p = Path.of(relative);
        if (java.nio.file.Files.exists(p)) return p;
        Path parent = Path.of("..", relative);
        if (java.nio.file.Files.exists(parent)) return parent;
        return p;
    }

    // --- Integration: Parse Example Files ---

    @Test
    void parseExamplePolicies_shouldWork() throws PolicyParseException {
        List<Policy> policies = policyParser.parseDirectoryRecursive(
                resolveExamplePath("policies/EU"));
        assertTrue(policies.size() >= 2, "Should find at least 2 example policies");
    }

    @Test
    void parseExampleServices_shouldWork() throws PolicyParseException {
        List<ServiceNode> services = serviceParser.parseFile(
                resolveExamplePath("examples/services.json"));
        assertEquals(3, services.size());
    }

    @Test
    void parseExampleValidDataFlows_shouldWork() throws PolicyParseException {
        List<DataFlowEdge> edges = dataFlowParser.parseFile(
                resolveExamplePath("examples/dataflows-valid.json"));
        assertEquals(1, edges.size());
    }

    @Test
    void parseExampleInvalidDataFlows_shouldWork() throws PolicyParseException {
        List<DataFlowEdge> edges = dataFlowParser.parseFile(
                resolveExamplePath("examples/dataflows-invalid.json"));
        assertEquals(2, edges.size());
    }
}
