package com.policymesh.servicegraph.repository;

import com.policymesh.servicegraph.entity.ServiceNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for ServiceNode entity.
 * Provides CRUD operations and custom query methods for service nodes.
 */
@Repository
public interface ServiceNodeRepository extends JpaRepository<ServiceNode, Long> {
    Optional<ServiceNode> findByName(String name);
}