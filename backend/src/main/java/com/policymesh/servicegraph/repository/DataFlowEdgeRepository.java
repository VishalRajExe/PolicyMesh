package com.policymesh.servicegraph.repository;

import com.policymesh.servicegraph.entity.DataFlowEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for DataFlowEdge entity.
 * Provides CRUD operations and custom query methods for data flow edges.
 */
@Repository
public interface DataFlowEdgeRepository extends JpaRepository<DataFlowEdge, Long> {
    List<DataFlowEdge> findBySourceServiceId(Long sourceServiceId);
    List<DataFlowEdge> findByDestinationServiceId(Long destinationServiceId);
    Optional<DataFlowEdge> findBySourceServiceIdAndDestinationServiceId(Long sourceServiceId, Long destinationServiceId);
}