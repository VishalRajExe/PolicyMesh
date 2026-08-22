package com.policymesh.servicegraph.service;

import com.policymesh.servicegraph.dto.DataFlowEdgeRequest;
import com.policymesh.servicegraph.dto.DataFlowEdgeResponse;

import java.util List;

/**
 * Service interface for DataFlowEdge operations.
 * Handles CRUD operations for data flow edges in the service mesh.
 */
public interface DataFlowEdgeService {

    /**
     * Creates a new data flow edge.
     *
     * @param request the data flow edge creation request
     * @return the created data flow edge response
     */
    DataFlowEdgeResponse createDataFlowEdge(DataFlowEdgeRequest request);

    /**
     * Retrieves a data flow edge by its ID.
     *
     * @param id the ID of the data flow edge to retrieve
     * @return the data flow edge response
     */
    DataFlowEdgeResponse getDataFlowEdgeById(Long id);

    /**
     * Retrieves all data flow edges.
     *
     * @return a list of all data flow edge responses
     */
    List<DataFlowEdgeResponse> getAllDataFlowEdges();

    /**
     * Updates an existing data flow edge.
     *
     * @param id the ID of the data flow edge to update
     * @param request the data flow edge update request
     * @return the updated data flow edge response
     */
    DataFlowEdgeResponse updateDataFlowEdge(Long id, DataFlowEdgeRequest request);

    /**
     * Deletes a data flow edge by its ID.
     *
     * @param id the ID of the data flow edge to delete
     */
    void deleteDataFlowEdge(Long id);
}