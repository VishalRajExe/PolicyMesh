package com.policymesh.policy.controller;

import com.policymesh.common.response.ApiResponse;
import com.policymesh.policy.dto.PolicyRequest;
import com.policymesh.policy.dto.PolicyResponse;
import com.policymesh.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/policies")
@RequiredArgsConstructor
@Validated
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    public ResponseEntity<ApiResponse<PolicyResponse>> createPolicy(@RequestBody PolicyRequest request) {
        PolicyResponse response = policyService.createPolicy(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Policy created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicyById(@PathVariable Long id) {
        PolicyResponse response = policyService.getPolicyById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{policyCode}")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicyByPolicyCode(@PathVariable String policyCode) {
        PolicyResponse response = policyService.getPolicyByPolicyCode(policyCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PolicyResponse>>> getAllPolicies() {
        List<PolicyResponse> responses = policyService.getAllPolicies();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PolicyResponse>> updatePolicy(@PathVariable Long id, @RequestBody PolicyRequest request) {
        PolicyResponse response = policyService.updatePolicy(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Policy updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Policy deleted successfully"));
    }
}