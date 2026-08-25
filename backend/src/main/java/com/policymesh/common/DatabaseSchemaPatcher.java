package com.policymesh.common;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaPatcher {

  private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaPatcher.class);
  private final DataSource dataSource;

  public DatabaseSchemaPatcher(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostConstruct
  public void patchSchema() {
    try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
      // Add updated_at column to users table if missing
      try {
        stmt.execute("ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        log.info("Successfully patched 'users' table with 'updated_at' column.");
      } catch (Exception e) {
        log.debug("Column 'updated_at' may already exist or table users not created yet: {}", e.getMessage());
      }

      // Ensure role column has sufficient length
      try {
        stmt.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(64) NOT NULL DEFAULT 'ENGINEER'");
      } catch (Exception e) {
        log.debug("Role column modify skipped: {}", e.getMessage());
      }
    } catch (Exception e) {
      log.warn("Database schema patcher encountered an issue (can be ignored on in-memory DBs): {}", e.getMessage());
    }
  }
}
