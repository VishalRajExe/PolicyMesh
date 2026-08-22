package com.policymesh.servicegraph.controller;

import com.policymesh.common.response.ApiResponse;
import com.policymesh.servicegraph.dto.ServiceNodeRequest;
import com.policymesh.servicegraph.dto.ServiceNodeResponse;
import com.policymesh.servicegraph.service.ServiceNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for ServiceNode operations.
 * Provides endpoints for managing service nodes in the service mesh.
 */
@RestController
@RequestMapping("${api.prefix}/services")
@RequiredArgsConstructor
@Validated
public class ServiceNodeController {

    private final ServiceNodeService serviceNodeService;

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceNodeResponse>> createServiceNode(@Valid @RequestBody ServiceNodeRequest request) {
        ServiceNodeResponse response = serviceNodeService.createServiceNode(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Service node created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceNodeResponse>> getServiceNodeById(@PathVariable Long id) {
        ServiceNodeResponse response = serviceNodeService.getServiceNodeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceNodeResponse>>> getAllServiceNodes() {
        List<ServiceNodeResponse> responses = serviceNodeService.getAllServiceNodes();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceNodeResponse>> updateServiceNode(@PathVariable Long id, @Valid @RequestBody ServiceNodeRequest request) {
        ServiceNodeResponse response = serviceNodeService.updateServiceNode(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Service node updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteServiceNode(@PathVariable Long id) {
        serviceNodeService.deleteServiceNode(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Service node deleted successfully"));
    }
}