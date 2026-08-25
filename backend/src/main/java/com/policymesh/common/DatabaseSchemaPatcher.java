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
      // 1. Patch 'updated_at' column
      try {
        stmt.execute("ALTER TABLE users ADD COLUMN updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)");
        log.info("Patched 'users' table with 'updated_at' column.");
      } catch (Exception e) {
        log.debug("Column 'updated_at' patch skipped: {}", e.getMessage());
      }

      // 2. Patch 'enabled' column with DEFAULT TRUE
      try {
        stmt.execute("ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE");
        log.info("Patched 'users' table with 'enabled' column.");
      } catch (Exception e) {
        try {
          stmt.execute("ALTER TABLE users MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE");
        } catch (Exception ignored) {}
      }

      // 3. Patch 'name' column
      try {
        stmt.execute("ALTER TABLE users ADD COLUMN name VARCHAR(255) NULL");
        log.info("Patched 'users' table with 'name' column.");
      } catch (Exception e) {
        log.debug("Column 'name' patch skipped: {}", e.getMessage());
      }

      // 4. Ensure 'status' column default
      try {
        stmt.execute("ALTER TABLE users ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'");
      } catch (Exception e) {
        try {
          stmt.execute("ALTER TABLE users MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'");
        } catch (Exception ignored) {}
      }

      // 5. Ensure 'role' column has sufficient length and default
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
