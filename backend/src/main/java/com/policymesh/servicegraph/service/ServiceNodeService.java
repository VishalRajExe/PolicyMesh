package com.policymesh.servicegraph.service;

import com.policymesh.servicegraph.dto.ServiceNodeRequest;
import com.policymesh.servicegraph.dto.ServiceNodeResponse;

import java.util.List;

/**
 * Service interface for ServiceNode operations.
 * Handles CRUD operations for service nodes in the service mesh.
 */
public interface ServiceNodeService {

    /**
     * Creates a new service node.
     *
     * @param request the service node creation request
     * @return the created service node response
     */
    ServiceNodeResponse createServiceNode(ServiceNodeRequest request);

    /**
     * Retrieves a service node by its ID.
     *
     * @param id the ID of the service node to retrieve
     * @return the service node response
     */
    ServiceNodeResponse getServiceNodeById(Long id);

    /**
     * Retrieves all service nodes.
     *
     * @return a list of all service node responses
     */
    List<ServiceNodeResponse> getAllServiceNodes();

    /**
     * Updates an existing service node.
     *
     * @param id the ID of the service node to update
     * @param request the service node update request
     * @return the updated service node response
     */
    ServiceNodeResponse updateServiceNode(Long id, ServiceNodeRequest request);

    /**
     * Deletes a service node by its ID.
     *
     * @param id the ID of the service node to delete
     */
    void deleteServiceNode(Long id);
}