package com.policymesh.ai;

import com.policymesh.common.ApiException;
import com.policymesh.events.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class ClassificationService {
  private static final Set<String> ALLOWED_CLASSES = Set.of("PII", "PCI", "PHI", "NON_SENSITIVE", "UNKNOWN");

  private final AIClassificationRepository repo;
  private final ClassificationProvider local;
  private final ClassificationProvider remote;
  private final EventPublisher events;
  private final boolean remoteEnabled;

  public ClassificationService(AIClassificationRepository repo,
                                LocalClassificationProvider local,
                                RemoteClassificationProvider remote,
                                EventPublisher events,
                                @Value("${policymesh.ai.mode:local}") String mode) {
    this.repo = repo;
    this.local = local;
    this.remote = remote;
    this.events = events;
    this.remoteEnabled = "remote".equalsIgnoreCase(mode == null ? "" : mode.trim());
  }

  public AiDtos.Response classify(AiDtos.Request r) {
    ClassificationProvider.Result result = remoteEnabled
        ? remote.classify(r.fieldName(), r.sampleValue())
        : local.classify(r.fieldName(), r.sampleValue());

    String rawClass = result.classification() == null ? "UNKNOWN" : result.classification().trim().toUpperCase();
    String validatedClass = ALLOWED_CLASSES.contains(rawClass) ? rawClass : "UNKNOWN";
    double boundedConfidence = Math.max(0.0, Math.min(1.0, result.confidence()));

    AIClassification a = new AIClassification();
    a.setFieldName(r.fieldName().trim());
    a.setSampleValue(r.sampleValue());
    a.setClassification(validatedClass);
    a.setConfidence(boundedConfidence);
    a.setStatus("PENDING");
    a.setProvider(result.provider());
    a.setReviewedBy(null);

    AIClassification saved = repo.save(a);
    return AiDtos.from(saved);
  }

  public AiDtos.Response approve(long id, String reviewerEmail) {
    AIClassification a = one(id);
    if ("APPROVED".equalsIgnoreCase(a.getStatus())) {
      return AiDtos.from(a); // Idempotent return
    }
    if ("REJECTED".equalsIgnoreCase(a.getStatus())) {
      throw ApiException.conflict("Classification has already been rejected and cannot be directly approved. Please submit a new classification request.");
    }
    if (!"PENDING".equalsIgnoreCase(a.getStatus())) {
      throw ApiException.conflict("Classification in state " + a.getStatus() + " cannot be approved.");
    }

    String reviewer = (reviewerEmail == null || reviewerEmail.isBlank()) ? "admin@policymesh.io" : reviewerEmail.trim();
    a.setStatus("APPROVED");
    a.setReviewedBy(reviewer);
    AIClassification saved = repo.save(a);

    Map<String, Object> payload = new HashMap<>();
    payload.put("id", saved.getId());
    payload.put("fieldName", saved.getFieldName());
    payload.put("classification", saved.getClassification());
    payload.put("status", "APPROVED");
    payload.put("reviewer", reviewer);
    events.publish(EventPublisher.TOPIC_AI_APPROVED, payload);

    return AiDtos.from(saved);
  }

  public AiDtos.Response reject(long id, String reviewerEmail) {
    AIClassification a = one(id);
    if ("REJECTED".equalsIgnoreCase(a.getStatus())) {
      return AiDtos.from(a); // Idempotent return
    }
    if ("APPROVED".equalsIgnoreCase(a.getStatus())) {
      throw ApiException.conflict("Classification has already been approved and cannot be directly rejected. Please submit a new classification request.");
    }
    if (!"PENDING".equalsIgnoreCase(a.getStatus())) {
      throw ApiException.conflict("Classification in state " + a.getStatus() + " cannot be rejected.");
    }

    String reviewer = (reviewerEmail == null || reviewerEmail.isBlank()) ? "admin@policymesh.io" : reviewerEmail.trim();
    a.setStatus("REJECTED");
    a.setReviewedBy(reviewer);
    AIClassification saved = repo.save(a);

    Map<String, Object> payload = new HashMap<>();
    payload.put("id", saved.getId());
    payload.put("fieldName", saved.getFieldName());
    payload.put("classification", saved.getClassification());
    payload.put("status", "REJECTED");
    payload.put("reviewer", reviewer);
    events.publish(EventPublisher.TOPIC_AI_REJECTED, payload);

    return AiDtos.from(saved);
  }

  @Transactional(readOnly = true)
  public List<AiDtos.Response> listAll() {
    return repo.findAllByOrderByCreatedAtDesc().stream().map(AiDtos::from).toList();
  }

  @Transactional(readOnly = true)
  public AiDtos.Response getById(long id) {
    return AiDtos.from(one(id));
  }

  /**
   * Resolves the effective, approved data class for a schema field.
   * If a human has approved it (status == APPROVED), returns that data class.
   * If pending, rejected, or unclassified, returns UNKNOWN.
   */
  @Transactional(readOnly = true)
  public String resolveEffectiveClass(String fieldName) {
    if (fieldName == null || fieldName.isBlank()) return "UNKNOWN";
    return repo.findFirstByFieldNameIgnoreCaseAndStatusOrderByCreatedAtDesc(fieldName.trim(), "APPROVED")
        .map(AIClassification::getClassification)
        .orElse("UNKNOWN");
  }

  private AIClassification one(long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("Classification not found"));
  }
}
