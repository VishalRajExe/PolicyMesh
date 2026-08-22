package com.policymesh.enforcement.repository;

import com.policymesh.enforcement.entity.Decision;
import com.policymesh.enforcement.entity.DecisionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, java.util.UUID> {
    long countByDecision(DecisionType decision);
    List<Decision> findAllByOrderByTimestampDesc(Pageable pageable);
}
