package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ClaimRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotBlank(message = "Issue category is required")
    private String category;

    @NotBlank(message = "Description is required")
    @Size(min = 8, message = "Description must be at least 8 characters long")
    private String description;

    private String mediaName;

    // Getters and Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMediaName() { return mediaName; }
    public void setMediaName(String mediaName) { this.mediaName = mediaName; }
}