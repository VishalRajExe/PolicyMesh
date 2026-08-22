package com.policymesh.enforcement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DecisionRepository extends JpaRepository<DecisionRecord, Long> {
  long countByDecision(String decision);
  long countByCreatedAtAfter(Instant since);
  List<DecisionRecord> findTop100ByOrderByCreatedAtDesc();
}
