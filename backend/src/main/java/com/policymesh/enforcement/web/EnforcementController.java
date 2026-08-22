package com.policymesh.enforcement.web;

import com.policymesh.enforcement.dto.EnforcementRequest;
import com.policymesh.enforcement.dto.EnforcementResponse;
import com.policymesh.enforcement.service.EnforcementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for enforcement operations.
 * Provides endpoints for checking if data transfers are allowed based on policies.
 */
@RestController
@RequestMapping("/api/v1/enforce")
@RequiredArgsConstructor
@Tag(name = "Enforcement", description = "APIs for checking data transfer enforcement")
public class EnforcementController {

    private final EnforcementService enforcementService;

    /**
     * Checks if a data transfer between services is allowed based on policies.
     *
     * @param request the enforcement check request containing source/destination details
     * @return the enforcement response with decision and lineage information
     */
    @Operation(
            summary = "Check enforcement for data transfer",
            description = "Evaluates if a data transfer between services is allowed based on policies"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Enforcement check completed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EnforcementResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/check")
    public ResponseEntity<EnforcementResponse> checkEnforcement(@RequestBody EnforcementRequest request) {
        EnforcementResponse response = enforcementService.checkEnforcement(request);
        return ResponseEntity.ok(response);
    }
}