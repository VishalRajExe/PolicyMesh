package com.policymesh.servicegraph.repository;

import com.policymesh.servicegraph.entity.ServiceNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceNodeRepository extends JpaRepository<ServiceNode, UUID> {
    Optional<ServiceNode> findByName(String name);
    boolean existsByName(String name);
}
