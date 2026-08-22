package com.policymesh.servicegraph.repository;

import com.policymesh.servicegraph.entity.DataFlowEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DataFlowEdgeRepository extends JpaRepository<DataFlowEdge, UUID> {
    List<DataFlowEdge> findBySourceId(UUID sourceId);
    List<DataFlowEdge> findByDestinationId(UUID destinationId);
}
