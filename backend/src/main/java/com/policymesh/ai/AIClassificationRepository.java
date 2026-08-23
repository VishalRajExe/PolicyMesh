package com.policymesh.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AIClassificationRepository extends JpaRepository<AIClassification, Long> {
  long countByStatus(String status);
  List<AIClassification> findAllByOrderByCreatedAtDesc();
  List<AIClassification> findByStatusOrderByCreatedAtDesc(String status);
  Optional<AIClassification> findFirstByFieldNameIgnoreCaseAndStatusOrderByCreatedAtDesc(String fieldName, String status);
}
