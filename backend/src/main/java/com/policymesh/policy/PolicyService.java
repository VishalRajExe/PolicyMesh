package com.policymesh.policy;

import com.policymesh.common.ApiException;
import com.policymesh.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Service
@Transactional
public class PolicyService {
  private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

  private final PolicyRepository repo;
  private final EventPublisher events;
  private final PolicyCache cache;
  private final com.policymesh.compiler.PolicyCompiler compiler;
  private final DataSource dataSource;

  /** Tracks whether we've already ensured the region tables exist in this JVM run. */
  private volatile boolean regionTablesEnsured = false;

  public PolicyService(PolicyRepository repo, EventPublisher events, PolicyCache cache,
                       com.policymesh.compiler.PolicyCompiler compiler,
                       DataSource dataSource) {
    this.repo = repo;
    this.events = events;
    this.cache = cache;
    this.compiler = compiler;
    this.dataSource = dataSource;
  }

  /**
   * Ensures policy_allowed_regions and policy_denied_regions tables exist.
   * Called before any write operation so the tables are guaranteed to exist
   * regardless of Flyway or Hibernate startup ordering.
   */
  private void ensureRegionTables() {
    if (regionTablesEnsured) return;
    try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute("CREATE TABLE IF NOT EXISTS policy_allowed_regions " +
          "(policy_id BIGINT NOT NULL, region VARCHAR(100) NOT NULL) " +
          "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
      stmt.execute("CREATE TABLE IF NOT EXISTS policy_denied_regions " +
          "(policy_id BIGINT NOT NULL, region VARCHAR(100) NOT NULL) " +
          "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
      regionTablesEnsured = true;
      log.warn("[PolicyService] Ensured policy region tables exist.");
    } catch (Exception e) {
      log.warn("[PolicyService] Region table ensure note: {}", e.getMessage());
      regionTablesEnsured = true; // don't retry on every request
    }
  }

  public List<PolicyDtos.Response> all() {
    return repo.findAll().stream().map(PolicyDtos::from).toList();
  }

  public PolicyDtos.Response one(long id) {
    return PolicyDtos.from(entity(id));
  }

  public PolicyDtos.Response create(PolicyDtos.Request r) {
    ensureRegionTables();
    Normalized n = normalize(r);
    if (repo.findByPolicyCodeIgnoreCase(n.policyCode()).isPresent()) {
      throw ApiException.conflict("Policy code already exists");
    }
    Policy p = new Policy();
    apply(p, n, r.status() != null ? r.status() : PolicyStatus.DRAFT);
    p = repo.save(p);
    changed(p);
    return PolicyDtos.from(p);
  }

  public PolicyDtos.Response createFromYaml(String yaml) {
    ensureRegionTables();
    var compiled = compiler.compile(yaml);
    if (repo.findByPolicyCodeIgnoreCase(compiled.policyCode()).isPresent()) {
      throw ApiException.conflict("Policy code already exists");
    }
    Policy p = new Policy();
    p.setPolicyCode(compiled.policyCode());
    p.setName(compiled.name());
    p.setJurisdiction(compiled.jurisdiction());
    p.setDataClass(compiled.dataClass());
    p.setAllowedRegions(compiled.allowedRegions());
    p.setDeniedRegions(compiled.deniedRegions());
    p.setStatus(compiled.status() != null ? compiled.status() : PolicyStatus.ACTIVE);
    p = repo.save(p);
    changed(p);
    return PolicyDtos.from(p);
  }

  public java.util.List<PolicyDtos.Response> importYaml(String yaml) {
    ensureRegionTables();
    var compiledList = compiler.compileAll(yaml);
    java.util.List<PolicyDtos.Response> results = new java.util.ArrayList<>();
    for (var compiled : compiledList) {
      Policy p = repo.findByPolicyCodeIgnoreCase(compiled.policyCode())
          .orElseGet(Policy::new);
      p.setPolicyCode(compiled.policyCode());
      p.setName(compiled.name());
      p.setJurisdiction(compiled.jurisdiction());
      p.setDataClass(compiled.dataClass());
      p.setAllowedRegions(compiled.allowedRegions());
      p.setDeniedRegions(compiled.deniedRegions());
      p.setStatus(compiled.status() != null ? compiled.status() : PolicyStatus.ACTIVE);
      p = repo.save(p);
      changed(p);
      results.add(PolicyDtos.from(p));
    }
    return results;
  }

  public PolicyDtos.Response update(long id, PolicyDtos.Request r) {
    Normalized n = normalize(r);
    Policy p = entity(id);
    repo.findByPolicyCodeIgnoreCase(n.policyCode())
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(existing -> { throw ApiException.conflict("Policy code already exists"); });
    if (p.getStatus() == PolicyStatus.INACTIVE) {
      throw ApiException.conflict("Policy is inactive and cannot be updated; create a new policy instead");
    }
    apply(p, n, r.status() != null ? r.status() : p.getStatus());
    p.setVersion(p.getVersion() + 1);
    p = repo.save(p);
    changed(p);
    return PolicyDtos.from(p);
  }

  /** Soft delete: a policy is deactivated (INACTIVE), never physically removed. */
  public void delete(long id) {
    Policy p = entity(id);
    p.setStatus(PolicyStatus.INACTIVE);
    p.setVersion(p.getVersion() + 1);
    repo.save(p);
    changed(p);
  }

  public Policy entity(long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("Policy not found"));
  }

  private void apply(Policy p, Normalized n, PolicyStatus status) {
    p.setPolicyCode(n.policyCode());
    p.setName(n.name());
    p.setJurisdiction(n.jurisdiction());
    p.setDataClass(n.dataClass());
    p.setAllowedRegions(n.allowedRegions());
    p.setDeniedRegions(n.deniedRegions());
    p.setStatus(status);
  }

  private void changed(Policy p) {
    cache.clear();
    Map<String, Object> payload = new HashMap<>();
    payload.put("policyId", p.getId());
    payload.put("policyCode", p.getPolicyCode());
    payload.put("version", p.getVersion());
    payload.put("status", p.getStatus().name());
    events.publish("policymesh.policy.updated", payload);
  }

  private Normalized normalize(PolicyDtos.Request r) {
    String policyCode = r.policyCode() == null ? "" : r.policyCode().trim().toUpperCase();
    if (!policyCode.matches("[A-Z0-9][A-Z0-9_-]*")) {
      throw ApiException.unprocessable("policyCode must match ^[A-Z0-9][A-Z0-9_-]*$");
    }
    String jurisdiction = PolicyVocabulary.canonicalRegion(r.jurisdiction());
    String dataClass = PolicyVocabulary.canonicalDataClass(r.dataClass());
    if (jurisdiction.isBlank()) throw ApiException.unprocessable("jurisdiction is required");
    if (!PolicyVocabulary.isKnownDataClass(dataClass)) {
      throw ApiException.unprocessable("Unknown dataClass '" + dataClass + "'; known classes: " + PolicyVocabulary.DATA_CLASSES);
    }
    TreeSet<String> allowed = regions(r.allowedRegions());
    TreeSet<String> denied = regions(r.deniedRegions());
    if (allowed.isEmpty()) throw ApiException.unprocessable("allowedRegions must contain at least one region");
    for (String region : allowed) {
      if (denied.contains(region)) {
        throw ApiException.unprocessable("allowedRegions and deniedRegions must not overlap (region " + region + ")");
      }
    }
    return new Normalized(policyCode, r.name().trim(), jurisdiction, dataClass, allowed, denied);
  }

  private TreeSet<String> regions(Set<String> regions) {
    TreeSet<String> result = new TreeSet<>();
    if (regions != null) {
      regions.stream().filter(Objects::nonNull)
          .map(PolicyVocabulary::canonicalRegion)
          .filter(region -> !region.isBlank())
          .forEach(result::add);
    }
    return result;
  }

  private record Normalized(String policyCode, String name, String jurisdiction, String dataClass,
                            TreeSet<String> allowedRegions, TreeSet<String> deniedRegions) {}
}
