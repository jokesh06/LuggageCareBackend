package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ClaimRequest {

    @NotNull(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Issue category is required")
    private String category;

    @NotBlank(message = "Description is required")
    @Size(min = 8, message = "Description must be at least 8 characters long")
    private String description;

    private String mediaName;

    private String method;


    // Getters and Setters
    public String getProductId() { return productId; }
    public void setProductId(@NotNull(message = "Product ID is required") String productId) { this.productId = productId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMediaName() { return mediaName; }
    public void setMediaName(String mediaName) { this.mediaName = mediaName; }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    
}