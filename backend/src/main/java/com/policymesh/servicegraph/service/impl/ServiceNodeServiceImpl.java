package com.policymesh.servicegraph.service.impl;

import com.policymesh.common.response.ApiResponse;
import com.policymesh.servicegraph.dto.ServiceNodeRequest;
import com.policymesh.servicegraph.dto.ServiceNodeResponse;
import com.policymesh.servicegraph.entity.ServiceNode;
import com.policymesh.servicegraph.repository.ServiceNodeRepository;
import com.policymesh.servicegraph.service.ServiceNodeService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the ServiceNodeService interface.
 * Handles CRUD operations for service nodes in the service mesh.
 */
@Service
@RequiredArgsConstructor
public class ServiceNodeServiceImpl implements ServiceNodeService {

    private final ServiceNodeRepository serviceNodeRepository;
    private final ModelMapper modelMapper;

    @Override
    public ServiceNodeResponse createServiceNode(ServiceNodeRequest request) {
        // Check if service node with same name already exists
        if (serviceNodeRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Service node with name " + request.getName() + " already exists");
        }

        ServiceNode serviceNode = modelMapper.map(request, ServiceNode.class);
        ServiceNode savedServiceNode = serviceNodeRepository.save(serviceNode);
        return modelMapper.map(savedServiceNode, ServiceNodeResponse.class);
    }

    @Override
    public ServiceNodeResponse getServiceNodeById(Long id) {
        ServiceNode serviceNode = serviceNodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service node not found with id: " + id));
        return modelMapper.map(serviceNode, ServiceNodeResponse.class);
    }

    @Override
    public List<ServiceNodeResponse> getAllServiceNodes() {
        List<ServiceNode> serviceNodes = serviceNodeRepository.findAll();
        return serviceNodes.stream()
                .map(serviceNode -> modelMapper.map(serviceNode, ServiceNodeResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public ServiceNodeResponse updateServiceNode(Long id, ServiceNodeRequest request) {
        ServiceNode existingServiceNode = serviceNodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service node not found with id: " + id));

        // Check if another service node with same name exists
        serviceNodeRepository.findByName(request.getName())
                .filter(serviceNode -> !serviceNode.getId().equals(id))
                .ifPresent(serviceNode -> {
                    throw new RuntimeException("Service node with name " + request.getName() + " already exists");
                });

        modelMapper.map(request, existingServiceNode);
        ServiceNode updatedServiceNode = serviceNodeRepository.save(existingServiceNode);
        return modelMapper.map(updatedServiceNode, ServiceNodeResponse.class);
    }

    @Override
    public void deleteServiceNode(Long id) {
        if (!serviceNodeRepository.existsById(id)) {
            throw new RuntimeException("Service node not found with id: " + id);
        }
        serviceNodeRepository.deleteById(id);
    }
}