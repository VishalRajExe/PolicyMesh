package com.policymesh;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Optional startup seeding, enabled with policymesh.demo.seed=true (POLICYMESH_DEMO_SEED). */
@Configuration
class DemoDataInitializer {
  @Bean
  CommandLineRunner seedDemo(@Value("${policymesh.demo.seed:false}") boolean enabled, DemoDataSeeder seeder) {
    return args -> {
      if (enabled) seeder.seedIfEmpty();
    };
  }
}
