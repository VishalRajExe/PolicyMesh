package com.policymesh.lineage;

import com.policymesh.common.ApiException;
import com.policymesh.enforcement.DecisionRecord;
import com.policymesh.events.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class LineageService {
  private final LineageRepository repo;
  private final EventPublisher events;

  public LineageService(LineageRepository repo, EventPublisher events) {
    this.repo = repo;
    this.events = events;
  }

  /**
   * Appends the decision to the chain: previousHash = tail's currentHash (null for the first
   * record), currentHash = SHA-256 over the canonical serialization. Serialized on the service
   * so concurrent decisions cannot fork the chain.
   */
  public synchronized LineageDtos.Response append(DecisionRecord decision) {
    LineageRecord r = new LineageRecord();
    r.setDecisionId(decision.getId());
    r.setSourceService(decision.getSourceService());
    r.setDestinationService(decision.getDestinationService());
    r.setSourceRegion(decision.getSourceRegion());
    r.setDestinationRegion(decision.getDestinationRegion());
    r.setDataClass(decision.getDataClass());
    r.setDecision(decision.getDecision());
    r.setPolicyId(decision.getPolicyId());
    r.setReason(decision.getReason());
    r.setPreviousHash(repo.findFirstByOrderByIdDesc().map(LineageRecord::getCurrentHash).orElse(null));
    r.setCurrentHash(hash(canonical(r)));
    r = repo.save(r);

    Map<String, Object> payload = new HashMap<>();
    payload.put("lineageId", r.getId());
    payload.put("currentHash", r.getCurrentHash());
    payload.put("decisionId", r.getDecisionId());
    events.publish(EventPublisher.TOPIC_LINEAGE_CREATED, payload);
    return LineageDtos.from(r);
  }

  @Transactional(readOnly = true)
  public List<LineageDtos.Response> all(String decision, String service) {
    return repo.findAllByOrderByIdAsc().stream()
        .map(LineageDtos::from)
        .filter(r -> decision == null || decision.isBlank() || r.decision().equalsIgnoreCase(decision.trim()))
        .filter(r -> service == null || service.isBlank()
            || r.sourceService().equalsIgnoreCase(service.trim())
            || r.destinationService().equalsIgnoreCase(service.trim()))
        .toList();
  }

  @Transactional(readOnly = true)
  public LineageDtos.Response one(long id) {
    return LineageDtos.from(repo.findById(id).orElseThrow(() -> ApiException.notFound("Lineage record not found")));
  }

  /**
   * Walks the chain in append order and re-derives every hash. Detects modified content,
   * a broken previousHash link, a forged currentHash and deleted/reordered records.
   */
  @Transactional(readOnly = true)
  public LineageDtos.Verification verify() {
    String expectedPrevious = null;
    long checked = 0;
    for (LineageRecord r : repo.findAllByOrderByIdAsc()) {
      checked++;
      if (!Objects.equals(expectedPrevious, r.getPreviousHash())) {
        return new LineageDtos.Verification(false, r.getId(), checked,
            "Broken previous-hash linkage at record " + r.getId() + ": expected "
                + (expectedPrevious == null ? "null" : expectedPrevious) + " but found " + r.getPreviousHash());
      }
      String recomputed = hash(canonical(r));
      if (!recomputed.equals(r.getCurrentHash())) {
        return new LineageDtos.Verification(false, r.getId(), checked,
            "Record " + r.getId() + " content or current hash was modified");
      }
      expectedPrevious = r.getCurrentHash();
    }
    return new LineageDtos.Verification(true, null, checked, "Lineage chain is valid");
  }

  /**
   * Deterministic canonical serialization (docs/LINEAGE_LEDGER.md field order, extended with
   * the regions this implementation records):
   * decisionId|source|destination|sourceRegion|destinationRegion|dataClass|decision|reason|policy|timestamp|previousHash
   */
  static String canonical(LineageRecord r) {
    return String.join("|",
        safe(r.getDecisionId() == null ? "" : r.getDecisionId().toString()),
        safe(r.getSourceService()),
        safe(r.getDestinationService()),
        safe(r.getSourceRegion()),
        safe(r.getDestinationRegion()),
        safe(r.getDataClass()),
        safe(r.getDecision()),
        safe(r.getReason()),
        safe(r.getPolicyId()),
        r.getCreatedAt().toString(),
        safe(r.getPreviousHash()));
  }

  private static String safe(String s) { return s == null ? "" : s; }

  static String hash(String s) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
