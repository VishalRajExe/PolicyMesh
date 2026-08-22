package com.policymesh.lineage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LineageRepository extends JpaRepository<LineageRecord, Long> {
  List<LineageRecord> findAllByOrderByIdAsc();
  Optional<LineageRecord> findFirstByOrderByIdDesc();
}
