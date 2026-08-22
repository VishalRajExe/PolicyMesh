package com.policymesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full REST walkthrough of the documented demo flow, using only the public API:
 * seed -> graph -> CI FAIL -> fix region -> CI PASS -> enforcement -> lineage -> dashboard -> AI.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "policymesh.kafka.enabled=false", "policymesh.redis.enabled=false",
    "policymesh.ai.mode=local"})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiEndpointsTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired MockMvc mvc;

  private String adminToken;

  private String admin() throws Exception {
    if (adminToken == null) {
      mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
          .content("{\"email\":\"admin@example.com\",\"password\":\"a-strong-password\",\"role\":\"ADMIN\"}"));
      MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
              .content("{\"email\":\"admin@example.com\",\"password\":\"a-strong-password\"}"))
          .andExpect(status().isOk()).andReturn();
      adminToken = JSON.readTree(login.getResponse().getContentAsString()).path("token").asText();
    }
    return adminToken;
  }

  @Test
  @Order(1)
  void seedsDemoDataAndValidatesGraph() throws Exception {
    String token = admin();
    mvc.perform(post("/api/v1/dev/seed").header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.policies").value(2))
        .andExpect(jsonPath("$.services").value(4))
        .andExpect(jsonPath("$.edges").value(3));

    mvc.perform(get("/api/v1/graph").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nodes.length()").value(4))
        .andExpect(jsonPath("$.edges.length()").value(3));

    mvc.perform(post("/api/v1/graph/validate").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("FAIL"))
        .andExpect(jsonPath("$.violationCount").value(1))
        .andExpect(jsonPath("$.violations[0].sourceService").value("orders-api"))
        .andExpect(jsonPath("$.violations[0].destinationService").value("analytics-api"))
        .andExpect(jsonPath("$.violations[0].policyCode").value("EU-PII-001"));
  }

  @Test
  @Order(2)
  void ciCheckFailsPersistsScanAndPassesAfterRegionFix() throws Exception {
    String token = admin();
    MvcResult failed = mvc.perform(post("/api/v1/ci/check").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"commitHash\":\"abc123\",\"branch\":\"main\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("FAIL"))
        .andExpect(jsonPath("$.violationCount").value(1))
        .andExpect(jsonPath("$.commitHash").value("abc123"))
        .andExpect(jsonPath("$.branch").value("main"))
        .andExpect(jsonPath("$.startedAt").isNotEmpty())
        .andExpect(jsonPath("$.completedAt").isNotEmpty())
        .andReturn();
    long scanId = JSON.readTree(failed.getResponse().getContentAsString()).path("id").asLong();

    mvc.perform(get("/api/v1/ci/scans/" + scanId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("FAIL"))
        .andExpect(jsonPath("$.violations.length()").value(1))
        .andExpect(jsonPath("$.violations[0].policyCode").value("EU-PII-001"));
    mvc.perform(get("/api/v1/ci/scans/999999").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());

    long analyticsId = serviceIdByName("analytics-api", token);
    mvc.perform(put("/api/v1/services/" + analyticsId).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"analytics-api\",\"region\":\"EU\",\"meshZone\":\"demo\",\"environment\":\"production\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.region").value("EU"));

    mvc.perform(post("/api/v1/ci/check").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"commitHash\":\"def456\",\"branch\":\"main\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("PASS"))
        .andExpect(jsonPath("$.violationCount").value(0))
        .andExpect(jsonPath("$.violations.length()").value(0));
  }

  @Test
  @Order(3)
  void enforcementProducesDecisionsAndLineageChain() throws Exception {
    String token = admin();
    mvc.perform(post("/api/v1/enforce/check").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sourceService\":\"orders-api\",\"destinationService\":\"analytics-api\",\"sourceRegion\":\"EU\",\"destinationRegion\":\"US\",\"dataClassTags\":[\"PII\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("DENY"))
        .andExpect(jsonPath("$.policyId").value("EU-PII-001"))
        .andExpect(jsonPath("$.reason").isNotEmpty())
        .andExpect(jsonPath("$.lineageHash").isNotEmpty())
        .andExpect(jsonPath("$.lineageId").isNumber())
        .andExpect(jsonPath("$.decisionId").isNumber());

    mvc.perform(post("/api/v1/enforce/check").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"source\":\"orders-api\",\"destination\":\"payments-api\",\"dataClass\":\"PII\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.decision").value("ALLOW"));

    MvcResult lineageList = mvc.perform(get("/api/v1/lineage").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk()).andReturn();
    JsonNode records = JSON.readTree(lineageList.getResponse().getContentAsString());
    assertThat(records.size()).isGreaterThanOrEqualTo(2);
    assertThat(records.get(1).path("previousHash").asText()).isEqualTo(records.get(0).path("currentHash").asText());
    long recordId = records.get(0).path("id").asLong();

    mvc.perform(get("/api/v1/lineage/" + recordId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(recordId));
    mvc.perform(get("/api/v1/lineage/999999").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());

    mvc.perform(get("/api/v1/lineage/verify").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.recordsChecked").value(records.size()));

    mvc.perform(get("/api/v1/audit/decisions").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].decision").isNotEmpty());
  }

  @Test
  @Order(4)
  void dashboardSummarizesTheSystemState() throws Exception {
    String token = admin();
    mvc.perform(get("/api/v1/dashboard/summary").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPolicies").value(2))
        .andExpect(jsonPath("$.totalServices").value(4))
        .andExpect(jsonPath("$.allowedTransfers").value(1))
        .andExpect(jsonPath("$.blockedTransfers").value(1))
        .andExpect(jsonPath("$.activeViolations").value(0)) // analytics moved to EU in step 2
        .andExpect(jsonPath("$.decisionsToday").value(2))
        .andExpect(jsonPath("$.lineageValid").value(true));
  }

  @Test
  @Order(5)
  void aiClassificationRequiresHumanApproval() throws Exception {
    String token = admin();
    MvcResult created = mvc.perform(post("/api/v1/ai/classify").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"fieldName\":\"email\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestedClass").value("PII"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.confidence").isNumber())
        .andReturn();
    long id = JSON.readTree(created.getResponse().getContentAsString()).path("id").asLong();

    mvc.perform(post("/api/v1/ai/classify/" + id + "/approve").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.reviewedBy").value("admin@example.com"));

    mvc.perform(post("/api/v1/ai/classify/" + id + "/reject").header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict()); // already reviewed
  }

  @Test
  @Order(6)
  void policyCrudValidationAndErrors() throws Exception {
    String token = admin();
    // No status in the body: POST must create a DRAFT per docs/API_SPEC.md.
    String policy = "{\"policyCode\":\"EU-PHI-001\",\"name\":\"EU PHI\",\"jurisdiction\":\"EU\",\"dataClass\":\"PHI\","
        + "\"allowedRegions\":[\"EU\"],\"deniedRegions\":[\"US\"]}";

    MvcResult created = mvc.perform(post("/api/v1/policies").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(policy))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.version").value(1))
        .andReturn();
    long id = JSON.readTree(created.getResponse().getContentAsString()).path("id").asLong();

    // Duplicate code -> 409.
    mvc.perform(post("/api/v1/policies").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(policy))
        .andExpect(status().isConflict());

    // Overlapping regions -> 422 problem+json.
    mvc.perform(post("/api/v1/policies").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"policyCode\":\"EU-X-001\",\"name\":\"x\",\"jurisdiction\":\"EU\",\"dataClass\":\"PII\",\"allowedRegions\":[\"EU\"],\"deniedRegions\":[\"EU\"],\"status\":\"ACTIVE\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("overlap")));

    // Unknown data class -> 422; empty allowedRegions -> 422; malformed JSON -> 400.
    mvc.perform(post("/api/v1/policies").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"policyCode\":\"EU-Y-001\",\"name\":\"x\",\"jurisdiction\":\"EU\",\"dataClass\":\"SECRET\",\"allowedRegions\":[\"EU\"],\"status\":\"ACTIVE\"}"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(post("/api/v1/policies").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"policyCode\":\"EU-Z-001\",\"name\":\"x\",\"jurisdiction\":\"EU\",\"dataClass\":\"PII\",\"allowedRegions\":[],\"status\":\"ACTIVE\"}"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(post("/api/v1/policies").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest());

    // Update activates the draft and increments the version; soft delete flips status and 404s on unknown id.
    mvc.perform(put("/api/v1/policies/" + id).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"policyCode\":\"EU-PHI-001\",\"name\":\"EU PHI\",\"jurisdiction\":\"EU\",\"dataClass\":\"PHI\",\"allowedRegions\":[\"EU\"],\"deniedRegions\":[\"US\"],\"status\":\"ACTIVE\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(2))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    mvc.perform(delete("/api/v1/policies/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/policies/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));
    mvc.perform(get("/api/v1/policies/999999").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
  }

  @Test
  @Order(7)
  void compilerEndpointCompilesAndRejects() throws Exception {
    String token = admin();
    mvc.perform(post("/api/v1/compiler/compile").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"yaml\":\"policy:\\n  id: EU-PII-001\\n  name: EU PII Protection\\n  jurisdiction: EU\\n  dataClass: PII\\n  allowedRegions: [EU]\\n  deniedRegions: [US, CN]\\n\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.policyCode").value("EU-PII-001"))
        .andExpect(jsonPath("$.dataClass").value("PII"));

    mvc.perform(post("/api/v1/compiler/compile").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"yaml\":\"policy: [broken\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/v1/compiler/compile").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"yaml\":\"policy:\\n  id: X-1\\n  name: x\\n  jurisdiction: EU\\n  dataClass: PII\\n  allowedRegions: [EU]\\n  deniedRegions: [EU]\\n\"}"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @Order(8)
  void edgeValidationRejectsUnknownServicesAndSelfLoops() throws Exception {
    String token = admin();
    mvc.perform(post("/api/v1/edges").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sourceServiceId\":1,\"destinationServiceId\":1,\"dataClasses\":[\"PII\"]}"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(post("/api/v1/edges").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sourceServiceId\":1,\"destinationServiceId\":999999,\"dataClasses\":[\"PII\"]}"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(get("/api/v1/services").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4));
    mvc.perform(get("/api/v1/edges").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
  }

  private long serviceIdByName(String name, String token) throws Exception {
    MvcResult result = mvc.perform(get("/api/v1/services").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk()).andReturn();
    for (JsonNode node : JSON.readTree(result.getResponse().getContentAsString())) {
      if (name.equals(node.path("name").asText())) return node.path("id").asLong();
    }
    throw new IllegalStateException("service not found: " + name);
  }
}
