package com.policymesh.ai.service;

import com.policymesh.ai.client.ClassificationClient;
import com.policymesh.ai.dto.ClassifyRequest;
import com.policymesh.ai.dto.ClassifyResponse;
import com.policymesh.ai.dto.FieldClassification;
import com.policymesh.ai.entity.AIClassification;
import com.policymesh.ai.repository.AIClassificationRepository;
import com.policymesh.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Suggests data classifications for fields via {@link ClassificationClient}
 * and persists them as pending review. Suggestions only become
 * enforcement-relevant once explicitly approved by a human
 * (COMPLIANCE_OFFICER/ADMIN) via {@link #approve}.
 */
@Service
@RequiredArgsConstructor
public class ClassificationService {

    private final ClassificationClient classificationClient;
    private final AIClassificationRepository repository;

    @Transactional
    public ClassifyResponse classify(ClassifyRequest request) {
        List<ClassificationClient.Suggestion> suggestions = classificationClient.classify(request.fields());

        List<FieldClassification> results = suggestions.stream().map(s -> {
            AIClassification entity = AIClassification.builder()
                    .fieldName(s.field())
                    .sampleValue(s.sampleValue())
                    .suggestedClass(s.classification())
                    .confidence(s.confidence())
                    .approved(false)
                    .rejected(false)
                    .build();
            AIClassification saved = repository.save(entity);
            return new FieldClassification(saved.getId(), saved.getFieldName(), saved.getSuggestedClass(),
                    saved.getConfidence(), saved.isApproved());
        }).toList();

        return new ClassifyResponse(results);
    }

    @Transactional
    public FieldClassification approve(UUID id) {
        AIClassification classification = getOrThrow(id);
        classification.setApproved(true);
        classification.setRejected(false);
        AIClassification saved = repository.save(classification);
        return new FieldClassification(saved.getId(), saved.getFieldName(), saved.getSuggestedClass(),
                saved.getConfidence(), saved.isApproved());
    }

    @Transactional
    public FieldClassification reject(UUID id) {
        AIClassification classification = getOrThrow(id);
        classification.setApproved(false);
        classification.setRejected(true);
        AIClassification saved = repository.save(classification);
        return new FieldClassification(saved.getId(), saved.getFieldName(), saved.getSuggestedClass(),
                saved.getConfidence(), saved.isApproved());
    }

    private AIClassification getOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AI classification not found: " + id));
    }
}
