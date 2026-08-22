package com.policymesh.lineage.repository;

import com.policymesh.lineage.entity.LineageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface LineageRecordRepository extends JpaRepository<LineageRecord, UUID> {

    Optional<LineageRecord> findTopByOrderBySequenceNoDesc();

    Stream<LineageRecord> findAllByOrderBySequenceNoAsc();

    long count();
}
