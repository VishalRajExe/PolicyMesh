package com.policymesh;

import com.policymesh.policy.Policy;
import com.policymesh.policy.PolicyRepository;
import com.policymesh.policy.PolicyStatus;
import com.policymesh.servicegraph.DataFlowEdge;
import com.policymesh.servicegraph.DataFlowEdgeRepository;
import com.policymesh.servicegraph.ServiceNode;
import com.policymesh.servicegraph.ServiceNodeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Demo seed reproducing the documented scenario:
 *   EU-PII-001 (PII: allow EU, deny US/CN) and IN-PII-001 (PII: allow IN, deny US/CN);
 *   web-app/orders-api/payments-api in EU, analytics-api in US;
 *   orders-api -> payments-api (PII, must ALLOW) and orders-api -> analytics-api (PII, must DENY).
 */
@Component
public class DemoDataSeeder {
  private final PolicyRepository policies;
  private final ServiceNodeRepository services;
  private final DataFlowEdgeRepository edges;

  public DemoDataSeeder(PolicyRepository policies, ServiceNodeRepository services, DataFlowEdgeRepository edges) {
    this.policies = policies;
    this.services = services;
    this.edges = edges;
  }

  @Transactional
  public Map<String, Integer> seedIfEmpty() {
    if (policies.count() > 0 || services.count() > 0) {
      return Map.of("seeded", 0);
    }
    savePolicy("EU-PII-001", "EU PII Protection", "EU", "PII", Set.of("EU"), Set.of("US", "CN"));
    savePolicy("IN-PII-001", "India PII Protection", "IN", "PII", Set.of("IN"), Set.of("US", "CN"));

    Map<String, ServiceNode> nodes = new TreeMap<>();
    for (String[] service : new String[][]{
        {"web-app", "EU"}, {"orders-api", "EU"}, {"payments-api", "EU"}, {"analytics-api", "US"}}) {
      ServiceNode node = new ServiceNode();
      node.setName(service[0]);
      node.setRegion(service[1]);
      node.setMeshZone("demo");
      node.setEnvironment("production");
      node.setDescription("Demo service " + service[0]);
      nodes.put(service[0], services.save(node));
    }

    for (String[] flow : new String[][]{
        {"web-app", "orders-api"}, {"orders-api", "payments-api"}, {"orders-api", "analytics-api"}}) {
      DataFlowEdge edge = new DataFlowEdge();
      edge.setSourceServiceId(nodes.get(flow[0]).getId());
      edge.setDestinationServiceId(nodes.get(flow[1]).getId());
      edge.setDataClasses(new TreeSet<>(Set.of("PII")));
      edges.save(edge);
    }
    return Map.of("policies", 2, "services", 4, "edges", 3);
  }

  private void savePolicy(String code, String name, String jurisdiction, String dataClass,
                          Set<String> allowed, Set<String> denied) {
    Policy p = new Policy();
    p.setPolicyCode(code);
    p.setName(name);
    p.setJurisdiction(jurisdiction);
    p.setDataClass(dataClass);
    p.setAllowedRegions(new TreeSet<>(allowed));
    p.setDeniedRegions(new TreeSet<>(denied));
    p.setStatus(PolicyStatus.ACTIVE);
    policies.save(p);
  }
}
