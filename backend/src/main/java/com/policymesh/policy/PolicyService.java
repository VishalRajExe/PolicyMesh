package com.policymesh.policy;

import com.policymesh.common.ApiException;
import com.policymesh.events.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Service
@Transactional
public class PolicyService {
  private final PolicyRepository repo;
  private final EventPublisher events;
  private final PolicyCache cache;

  public PolicyService(PolicyRepository repo, EventPublisher events, PolicyCache cache) {
    this.repo = repo;
    this.events = events;
    this.cache = cache;
  }

  public List<PolicyDtos.Response> all() {
    return repo.findAll().stream().map(PolicyDtos::from).toList();
  }

  public PolicyDtos.Response one(long id) {
    return PolicyDtos.from(entity(id));
  }

  public PolicyDtos.Response create(PolicyDtos.Request r) {
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
