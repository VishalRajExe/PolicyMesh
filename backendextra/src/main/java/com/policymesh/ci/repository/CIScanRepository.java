package com.policymesh.ci.repository;

import com.policymesh.ci.entity.CIScan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CIScanRepository extends JpaRepository<CIScan, UUID> {
}
