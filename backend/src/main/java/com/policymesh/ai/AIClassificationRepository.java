package com.policymesh.ai;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AIClassificationRepository extends JpaRepository<AIClassification, Long> {
  long countByStatus(String status);
}
