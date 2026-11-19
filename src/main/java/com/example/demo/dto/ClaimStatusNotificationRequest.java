package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Body Zoho Creator sends when a claim's status changes.
 *
 * {
 *   "customerId": "87676000000076001",   // Catalyst_ID field in Creator
 *   "claimId":    "LC-2627862",          // luggage_service_request_name
 *   "status":     "Under review",        // new request_status
 *   "title":      "optional custom title",
 *   "message":    "optional custom message"
 * }
 */
public class ClaimStatusNotificationRequest {

    @NotBlank(message = "customerId is required")
    @Pattern(regexp = "\\d+", message = "customerId must be numeric")
    private String customerId;

    @NotBlank(message = "claimId is required")
    private String claimId;

    @NotBlank(message = "status is required")
    private String status;

    private String title;

    private String message;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId == null ? null : customerId.trim(); }

    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
