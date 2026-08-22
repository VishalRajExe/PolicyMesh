package com.policymesh.config.seed;

import com.policymesh.policy.entity.Policy;
import com.policymesh.policy.entity.PolicyStatus;
import com.policymesh.policy.repository.PolicyRepository;
import com.policymesh.servicegraph.entity.DataFlowEdge;
import com.policymesh.servicegraph.entity.ServiceNode;
import com.policymesh.servicegraph.repository.DataFlowEdgeRepository;
import com.policymesh.servicegraph.repository.ServiceNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Seeds the acceptance-test demo dataset described in the spec:
 *  - Policies: EU-PII-001, IN-PII-001
 *  - Services: web-app(EU), orders-api(EU), payments-api(EU), analytics-api(US)
 *  - Edges: web-app->orders-api, orders-api->payments-api, orders-api->analytics-api
 *
 * Enabled via policymesh.seed.enabled=true (see application-dev.properties)
 * or the ./scripts/seed-demo-data script hitting the same code path through
 * `--seed` at startup. Idempotent: skips seeding if data already exists.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final PolicyRepository policyRepository;
    private final ServiceNodeRepository serviceNodeRepository;
    private final DataFlowEdgeRepository dataFlowEdgeRepository;

    @Value("${policymesh.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean forceSeed = args.getOptionNames().contains("seed") || args.getNonOptionArgs().contains("seed");

        if (!seedEnabled && !forceSeed) {
            return;
        }

        if (policyRepository.count() > 0 || serviceNodeRepository.count() > 0) {
            log.info("Demo data already present; skipping seed.");
            return;
        }

        log.info("Seeding PolicyMesh demo data...");

        Policy euPii = policyRepository.save(Policy.builder()
                .policyCode("EU-PII-001")
                .name("EU PII Protection")
                .jurisdiction("EU")
                .dataClass("PII")
                .allowedRegions("EU")
                .deniedRegions("US,CN")
                .status(PolicyStatus.ACTIVE)
                .build());

        Policy inPii = policyRepository.save(Policy.builder()
                .policyCode("IN-PII-001")
                .name("India PII Protection")
                .jurisdiction("INDIA")
                .dataClass("PII")
                .allowedRegions("IN")
                .status(PolicyStatus.ACTIVE)
                .build());

        ServiceNode webApp = serviceNodeRepository.save(ServiceNode.builder()
                .name("web-app").region("EU").environment("production").build());
        ServiceNode ordersApi = serviceNodeRepository.save(ServiceNode.builder()
                .name("orders-api").region("EU").environment("production").build());
        ServiceNode paymentsApi = serviceNodeRepository.save(ServiceNode.builder()
                .name("payments-api").region("EU").environment("production").build());
        ServiceNode analyticsApi = serviceNodeRepository.save(ServiceNode.builder()
                .name("analytics-api").region("US").environment("production").build());

        dataFlowEdgeRepository.save(DataFlowEdge.builder()
                .source(webApp).destination(ordersApi).dataClasses("PII").build());
        dataFlowEdgeRepository.save(DataFlowEdge.builder()
                .source(ordersApi).destination(paymentsApi).dataClasses("PII").build());
        dataFlowEdgeRepository.save(DataFlowEdge.builder()
                .source(ordersApi).destination(analyticsApi).dataClasses("PII").build());

        log.info("Demo data seeded: {} policies, {} services, {} edges",
                policyRepository.count(), serviceNodeRepository.count(), dataFlowEdgeRepository.count());
    }
}
