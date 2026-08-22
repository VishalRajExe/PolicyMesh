package com.policymesh.servicegraph.service;

import com.policymesh.common.exception.DuplicateResourceException;
import com.policymesh.common.exception.ResourceNotFoundException;
import com.policymesh.servicegraph.dto.ServiceNodeRequest;
import com.policymesh.servicegraph.dto.ServiceNodeResponse;
import com.policymesh.servicegraph.entity.ServiceNode;
import com.policymesh.servicegraph.repository.ServiceNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceNodeService {

    private final ServiceNodeRepository serviceNodeRepository;

    @Transactional(readOnly = true)
    public List<ServiceNodeResponse> findAll() {
        return serviceNodeRepository.findAll().stream().map(ServiceNodeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceNodeResponse findById(UUID id) {
        return ServiceNodeResponse.from(getOrThrow(id));
    }

    @Transactional
    public ServiceNodeResponse create(ServiceNodeRequest request) {
        if (serviceNodeRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Service '" + request.name() + "' already exists");
        }
        ServiceNode node = ServiceNode.builder()
                .name(request.name())
                .region(request.region().toUpperCase())
                .meshZone(request.meshZone())
                .environment(request.environment() == null ? "production" : request.environment())
                .description(request.description())
                .build();
        return ServiceNodeResponse.from(serviceNodeRepository.save(node));
    }

    @Transactional
    public ServiceNodeResponse update(UUID id, ServiceNodeRequest request) {
        ServiceNode node = getOrThrow(id);
        if (!node.getName().equals(request.name()) && serviceNodeRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Service '" + request.name() + "' already exists");
        }
        node.setName(request.name());
        node.setRegion(request.region().toUpperCase());
        node.setMeshZone(request.meshZone());
        node.setEnvironment(request.environment() == null ? node.getEnvironment() : request.environment());
        node.setDescription(request.description());
        return ServiceNodeResponse.from(serviceNodeRepository.save(node));
    }

    @Transactional
    public void delete(UUID id) {
        ServiceNode node = getOrThrow(id);
        serviceNodeRepository.delete(node);
    }

    ServiceNode getOrThrow(UUID id) {
        return serviceNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id));
    }
}
