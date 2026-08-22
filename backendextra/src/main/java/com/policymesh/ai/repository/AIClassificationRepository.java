package com.policymesh.ai.repository;

import com.policymesh.ai.entity.AIClassification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AIClassificationRepository extends JpaRepository<AIClassification, UUID> {
}
