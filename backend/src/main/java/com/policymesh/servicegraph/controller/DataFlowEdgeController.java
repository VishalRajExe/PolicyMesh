package com.policymesh.servicegraph.controller;

import com.policymesh.common.response.ApiResponse;
import com.policymesh.servicegraph.dto.DataFlowEdgeRequest;
import com.policymesh.servicegraph.dto.DataFlowEdgeResponse;
import com.policymesh.servicegraph.service.DataFlowEdgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util List;

/**
 * REST controller for DataFlowEdge operations.
 * Provides endpoints for managing data flow edges in the service mesh.
 */
@RestController
@RequestMapping("${api.prefix}/data-flow-edges")
@RequiredArgsConstructor
@Validated
public class DataFlowEdgeController {

    private final DataFlowEdgeService dataFlowEdgeService;

    @PostMapping
    public ResponseEntity<ApiResponse<DataFlowEdgeResponse>> createDataFlowEdge(@Valid @RequestBody DataFlowEdgeRequest request) {
        DataFlowEdgeResponse response = dataFlowEdgeService.createDataFlowEdge(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Data flow edge created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DataFlowEdgeResponse>> getDataFlowEdgeById(@PathVariable Long id) {
        DataFlowEdgeResponse response = dataFlowEdgeService.getDataFlowEdgeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DataFlowEdgeResponse>>> getAllDataFlowEdges() {
        List<DataFlowEdgeResponse> responses = dataFlowEdgeService.getAllDataFlowEdges();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DataFlowEdgeResponse>> updateDataFlowEdge(@PathVariable Long id, @Valid @RequestBody DataFlowEdgeRequest request) {
        DataFlowEdgeResponse response = dataFlowEdgeService.updateDataFlowEdge(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Data flow edge updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDataFlowEdge(@PathVariable Long id) {
        dataFlowEdgeService.deleteDataFlowEdge(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Data flow edge deleted successfully"));
    }
}