package com.policymesh;

import com.policymesh.auth.JwtService;
import com.policymesh.auth.Role;
import com.policymesh.auth.User;
import com.policymesh.auth.UserRepository;
import com.policymesh.policy.Policy;
import com.policymesh.policy.PolicyRepository;
import com.policymesh.policy.PolicyStatus;
import com.policymesh.servicegraph.DataFlowEdge;
import com.policymesh.servicegraph.DataFlowEdgeRepository;
import com.policymesh.servicegraph.ServiceNode;
import com.policymesh.servicegraph.ServiceNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GraphReEvaluateIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ServiceNodeRepository serviceRepo;
  @Autowired private DataFlowEdgeRepository edgeRepo;
  @Autowired private PolicyRepository policyRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private JwtService jwtService;
  @Autowired private PasswordEncoder encoder;

  private String token;
  private ServiceNode ordersService;
  private ServiceNode analyticsService;
  private DataFlowEdge flowEdge;

  @BeforeEach
  void setup() {
    edgeRepo.deleteAll();
    serviceRepo.deleteAll();
    policyRepo.deleteAll();
    userRepo.deleteAll();

    User u = new User();
    u.setEmail("admin@policymesh.io");
    u.setPasswordHash(encoder.encode("admin123"));
    u.setRole(Role.ADMIN);
    userRepo.save(u);
    token = "Bearer " + jwtService.issue(u);

    // Seed EU-PII-001 Policy
    Policy p = new Policy();
    p.setPolicyCode("EU-PII-001");
    p.setName("EU Personal Data Residency");
    p.setJurisdiction("EU");
    p.setDataClass("PII");
    p.setAllowedRegions(Set.of("EU"));
    p.setDeniedRegions(Set.of("US"));
    p.setStatus(PolicyStatus.ACTIVE);
    policyRepo.save(p);

    // Seed services: orders-api (EU), analytics-api (US)
    ordersService = new ServiceNode();
    ordersService.setName("orders-api");
    ordersService.setRegion("EU");
    ordersService.setMeshZone("zone-eu-west-1");
    ordersService.setEnvironment("production");
    ordersService = serviceRepo.save(ordersService);

    analyticsService = new ServiceNode();
    analyticsService.setName("analytics-api");
    analyticsService.setRegion("US");
    analyticsService.setMeshZone("zone-us-east-1");
    analyticsService.setEnvironment("production");
    analyticsService = serviceRepo.save(analyticsService);

    // Seed Edge: orders-api -> analytics-api with PII
    flowEdge = new DataFlowEdge();
    flowEdge.setSourceServiceId(ordersService.getId());
    flowEdge.setDestinationServiceId(analyticsService.getId());
    flowEdge.setDataClasses(Set.of("PII"));
    flowEdge = edgeRepo.save(flowEdge);
  }

  @Test
  @DisplayName("Re-evaluate Graph dynamically detects topology mutations in real time")
  void testDynamicGraphReEvaluation() throws Exception {
    // 1. Initial State (analytics-api is in US) -> Violation Expected
    mvc.perform(post("/api/v1/graph/re-evaluate")
            .header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("FAIL"))
        .andExpect(jsonPath("$.violationCount").value(1))
        .andExpect(jsonPath("$.checkedEdges").value(1))
        .andExpect(jsonPath("$.totalFlows").value(1))
        .andExpect(jsonPath("$.compliantFlows").value(0))
        .andExpect(jsonPath("$.violations[0].sourceService").value("orders-api"))
        .andExpect(jsonPath("$.violations[0].destinationService").value("analytics-api"))
        .andExpect(jsonPath("$.violations[0].policyCode").value("EU-PII-001"));

    // 2. Change analytics-api region: US -> EU
    analyticsService.setRegion("EU");
    serviceRepo.save(analyticsService);

    // 3. Re-evaluate Graph -> Compliant Expected
    mvc.perform(post("/api/v1/graph/re-evaluate")
            .header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("PASS"))
        .andExpect(jsonPath("$.violationCount").value(0))
        .andExpect(jsonPath("$.checkedEdges").value(1))
        .andExpect(jsonPath("$.compliantFlows").value(1))
        .andExpect(jsonPath("$.violations.length()").value(0));

    // 4. Change analytics-api region back to US
    analyticsService.setRegion("US");
    serviceRepo.save(analyticsService);

    // 5. Re-evaluate Graph again -> Violation Expected
    mvc.perform(post("/api/v1/graph/validate")
            .header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("FAIL"))
        .andExpect(jsonPath("$.violationCount").value(1))
        .andExpect(jsonPath("$.compliantFlows").value(0));
  }
}
