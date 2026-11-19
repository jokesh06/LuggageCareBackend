package com.example.demo.controller;

import com.example.demo.dto.ClaimRequest;
import com.example.demo.service.ClaimQueryService;
import com.example.demo.service.ClaimService;
import com.example.demo.service.ZohoCreatorClaimService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ClaimController {

    private final ClaimService claimService;
                   private final ClaimQueryService claimQueryService;
    private final ZohoCreatorClaimService zohoCreatorClaimService;

    public ClaimController(ClaimService claimService, ClaimQueryService claimQueryService,
                           ZohoCreatorClaimService zohoCreatorClaimService) {
        this.claimService = claimService;
        this.claimQueryService = claimQueryService;
        this.zohoCreatorClaimService = zohoCreatorClaimService;
    }

    @PostMapping("/customer/{customerId}")
    public ResponseEntity<Map<String,Object>> registerClaim(
            @PathVariable Long customerId,
            @Validated @RequestBody ClaimRequest request) {
        try {
            Map<String, Object> createdClaim = claimService.raiseClaim(customerId, request);
            return ResponseEntity.ok(createdClaim);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create service claim: " + e.getMessage()));
        }
    }
    @SuppressWarnings("rawtypes")
    @GetMapping("/customer/{customerId}/open")
    public ResponseEntity<?> getOpenClaims(
            @PathVariable Long customerId) {
        try {
            // Old: read open claims from Catalyst Data Store
            // List openClaims = claimQueryService.getOpenClaimsForCustomer(customerId);

            // New: read open claims from Zoho Creator
            List openClaims = zohoCreatorClaimService.getOpenClaimsFromCreator(customerId);
            return ResponseEntity.ok(openClaims);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }
    @SuppressWarnings("rawtypes")
    @GetMapping("/customer/{customerId}/closed")
    public ResponseEntity<?> getClosedClaims(
            @PathVariable Long customerId) {
        try {
            // Old: read closed claims from Catalyst Data Store
            // List closedClaims = claimQueryService.getClosedClaimsForCustomer(customerId);

            // New: read closed claims from Zoho Creator
            List closedClaims = zohoCreatorClaimService.getClosedClaimsFromCreator(customerId);
            return ResponseEntity.ok(closedClaims);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }
}