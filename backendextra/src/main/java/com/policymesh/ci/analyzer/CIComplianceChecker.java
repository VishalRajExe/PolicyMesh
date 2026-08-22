package com.policymesh.ci.analyzer;

import com.policymesh.ci.dto.CIScanRequest;
import com.policymesh.ci.service.CIScanService;
import com.policymesh.graph.model.GraphCheckResult;
import com.policymesh.graph.model.GraphViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CI checker, independently runnable from the REST server:
 *
 *   java -jar policymesh.jar check [--branch=main] [--commit=abcd123]
 *
 * or, via Maven:
 *
 *   mvn spring-boot:run -Dspring-boot.run.arguments=check
 *
 * Loads policies, builds the service data-flow graph, evaluates every
 * edge, prints a human-readable console report, and exits non-zero when
 * violations are found so CI systems (e.g. GitHub Actions) mark the run
 * as failed.
 *
 * Only activates when "check" is passed as a program argument, so the
 * normal `mvn spring-boot:run` (server mode) is unaffected.
 */
@Component
@RequiredArgsConstructor
public class CIComplianceChecker implements ApplicationRunner {

    private final CIScanService ciScanService;
    private final ConfigurableApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        if (!args.getNonOptionArgs().contains("check")) {
            return;
        }

        String branch = firstOptionValue(args, "branch");
        String commit = firstOptionValue(args, "commit");

        System.out.println();
        System.out.println("PolicyMesh Compliance Check");
        System.out.println();
        System.out.println("Loading policies...");
        System.out.println("Loading service graph...");

        var response = ciScanService.runScan(new CIScanRequest(commit, branch));
        GraphCheckResult result = response.result();

        System.out.println("Analyzing " + result.servicesAnalyzed() + " services...");
        System.out.println("Analyzing " + result.edgesAnalyzed() + " data-flow edges...");
        System.out.println();

        List<GraphViolation> violations = result.violations();
        for (GraphViolation v : violations) {
            System.out.printf("[FAIL] %s %s -> %s %s%n",
                    v.sourceService(), v.sourceRegion(), v.destinationService(), v.destinationRegion());
        }

        if (violations.isEmpty()) {
            System.out.println("[PASS] All data-flow edges comply with policy");
            System.out.println();
            System.out.println("✅ COMPLIANCE CHECK PASSED");
            exit(0);
        } else {
            System.out.println();
            for (GraphViolation v : violations) {
                System.out.println("Policy: " + v.policyCode());
                System.out.println("Data Class: " + v.dataClass());
                System.out.println();
                System.out.println("Reason:");
                System.out.println(v.reason());
                System.out.println();
            }
            System.out.println("❌ POLICY VIOLATION");
            System.out.println();
            System.out.println("Compliance Check: FAILED");
            System.out.println("Violations: " + violations.size());
            exit(1);
        }
    }

    private void exit(int code) {
        System.exit(SpringApplication.exit(context, () -> code));
    }

    private String firstOptionValue(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
