package com.policymesh.ai;

import com.policymesh.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClassificationService {
  private final AIClassificationRepository repo;
  private final ClassificationProvider local;
  private final ClassificationProvider remote;
  private final boolean remoteEnabled;

  public ClassificationService(AIClassificationRepository repo,
                                LocalClassificationProvider local,
                                RemoteClassificationProvider remote,
                                @Value("${policymesh.ai.mode:local}") String mode) {
    this.repo = repo;
    this.local = local;
    this.remote = remote;
    this.remoteEnabled = "remote".equalsIgnoreCase(mode == null ? "" : mode.trim());
  }

  public AiDtos.Response classify(AiDtos.Request r) {
    ClassificationProvider.Result result = remoteEnabled
        ? remote.classify(r.fieldName(), r.sampleValue())
        : local.classify(r.fieldName(), r.sampleValue());
    AIClassification a = new AIClassification();
    a.setFieldName(r.fieldName().trim());
    a.setSampleValue(r.sampleValue());
    a.setClassification(result.classification());
    a.setConfidence(result.confidence());
    a.setProvider(result.provider());
    return AiDtos.from(repo.save(a));
  }

  public AiDtos.Response approve(long id, String reviewerEmail) {
    return review(id, "APPROVED", reviewerEmail);
  }

  public AiDtos.Response reject(long id, String reviewerEmail) {
    return review(id, "REJECTED", reviewerEmail);
  }

  private AiDtos.Response review(long id, String targetStatus, String reviewerEmail) {
    AIClassification a = one(id);
    if (!"PENDING".equals(a.getStatus())) {
      throw ApiException.conflict("Classification already " + a.getStatus().toLowerCase());
    }
    a.setStatus(targetStatus);
    a.setReviewedBy(reviewerEmail);
    return AiDtos.from(repo.save(a));
  }

  private AIClassification one(long id) {
    return repo.findById(id).orElseThrow(() -> ApiException.notFound("Classification not found"));
  }
}
