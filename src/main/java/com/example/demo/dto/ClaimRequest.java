package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Matches the claim payload sent by the Flutter app:
 * claimId, customerId, productId, brand, modelNumber, serialNumber, category,
 * description, stage, method, center, quote, approved, mediaName, feedbackGiven.
 */
public class ClaimRequest {

    private String claimId;

    private Long customerId;

    @NotNull(message = "Product ID is required")
    private String productId;

    private String brand;

    private String modelNumber;

    private String serialNumber;

    @NotBlank(message = "Issue category is required")
    private String category;

    @NotBlank(message = "Description is required")
    @Size(min = 8, message = "Description must be at least 8 characters long")
    private String description;

    private String stage;

    private String method;

    private String center;

    private Double quote;

    private Boolean approved;

    private String mediaName;

    private Boolean feedbackGiven;

    /** Returns the stage sent by the client, or "Claim submitted" if none was provided. */
    public String resolveStage() {
        return (stage == null || stage.isBlank()) ? "Claim submitted" : stage.trim();
    }

    /** Returns the service method sent by the client, or "Store drop-off" if none was provided. */
    public String resolveMethod() {
        return (method == null || method.isBlank()) ? "Store drop-off" : method.trim();
    }

    /** Returns the service center sent by the client, or "" if none was provided. */
    public String resolveCenter() {
        return center == null ? "" : center.trim();
    }

    // Getters and Setters
    public String getClaimId() { return claimId; }
    public void setClaimId(String claimId) { this.claimId = claimId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModelNumber() { return modelNumber; }
    public void setModelNumber(String modelNumber) { this.modelNumber = modelNumber; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getCenter() { return center; }
    public void setCenter(String center) { this.center = center; }

    public Double getQuote() { return quote; }
    public void setQuote(Double quote) { this.quote = quote; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public String getMediaName() { return mediaName; }
    public void setMediaName(String mediaName) { this.mediaName = mediaName; }

    public Boolean getFeedbackGiven() { return feedbackGiven; }
    public void setFeedbackGiven(Boolean feedbackGiven) { this.feedbackGiven = feedbackGiven; }
}
