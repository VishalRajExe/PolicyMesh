package com.policymesh;

import com.policymesh.ci.CiService;
import com.policymesh.ci.ComplianceCli;
import com.policymesh.ci.CiDtos;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class PolicyMeshApplication {

  public static void main(String[] args) {
    if (args.length > 0 && "check".equalsIgnoreCase(args[0])) {
      String[] rest = Arrays.copyOfRange(args, 1, args.length);
      if (ComplianceCli.isFilesMode(rest)) {
        System.exit(ComplianceCli.runFiles(rest));
      }
      System.exit(runDatabaseCheck(rest));
    }
    SpringApplication.run(PolicyMeshApplication.class, args);
  }

  /**
   * Database-backed check: analyzes the registered service graph through the normal
   * CI stack (GraphAnalyzer -> PolicyEngine) and persists the scan. Exit 0 = PASS,
   * 1 = violations, anything else = infrastructure failure.
   */
  private static int runDatabaseCheck(String[] args) {
    ConfigurableApplicationContext ctx = new SpringApplicationBuilder(PolicyMeshApplication.class)
        .web(WebApplicationType.NONE)
        .bannerMode(Banner.Mode.OFF)
        .run(args);
    try {
      CiDtos.Response scan = ctx.getBean(CiService.class).run(
          envOrDefault(System.getenv("GITHUB_REF_NAME"), "local"),
          envOrDefault(System.getenv("GITHUB_SHA"), "HEAD"));
      System.out.println(scan.humanReadable());
      scan.violations().forEach(v -> System.out.println("  x " + v.sourceService() + " [" + v.sourceRegion()
          + "] -> " + v.destinationService() + " [" + v.destinationRegion() + "] dataClass=" + v.dataClass()
          + (v.policyCode() != null ? " policy=" + v.policyCode() : "") + "\n      " + v.reason()));
      return scan.passed() ? 0 : 1;
    } finally {
      ctx.close();
    }
  }

  private static String envOrDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
