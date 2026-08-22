package com.policymesh.dev;

import com.policymesh.DemoDataSeeder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Idempotent demo-data endpoint referenced by docs/LOCAL_DEVELOPMENT.md.
 * Requires ADMIN and can be disabled entirely with policymesh.demo.seed-endpoint-enabled=false.
 */
@RestController
@RequestMapping({"/api/v1/dev", "/api/v1/demo"})
public class DevSeedController {
  private final DemoDataSeeder seeder;
  private final boolean enabled;

  public DevSeedController(DemoDataSeeder seeder,
                           @Value("${policymesh.demo.seed-endpoint-enabled:true}") boolean enabled) {
    this.seeder = seeder;
    this.enabled = enabled;
  }

  @PostMapping("/seed")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Integer> seed() {
    if (!enabled) {
      throw new com.policymesh.common.ApiException(HttpStatus.NOT_FOUND, "Seed endpoint is disabled");
    }
    return seeder.seedIfEmpty();
  }
}
