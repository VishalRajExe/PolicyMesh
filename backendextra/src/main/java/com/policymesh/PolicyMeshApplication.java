package com.policymesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * PolicyMesh - Policy-as-code platform for compliance-as-code and
 * data-residency enforcement.
 *
 * The same declarative policy is compiled once and then used for:
 *  1. CI-time data-flow graph validation (build-time)
 *  2. Runtime data-flow enforcement (ALLOW / DENY / REROUTE)
 *  3. Hash-chained lineage records for audit evidence
 */
@SpringBootApplication
@EnableAsync
public class PolicyMeshApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolicyMeshApplication.class, args);
    }
}
