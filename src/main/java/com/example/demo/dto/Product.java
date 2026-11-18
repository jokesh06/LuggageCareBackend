package com.example.demo.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * API-facing product model. Populated either from the Catalyst DataStore
 * (ProductService) or from the Zoho CRM "Luggage_Registrations" module
 * (ZohoCrmService) via explicit mapping.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {

    private String rowId;
    private String customerId;
    private String productId;
    private String name;
    private String brand;
    private String serialNumber;
    private LocalDate purchaseDate;
    private String purchaseChannel;
    private String coveragePeriod;
    private String warrantyStatus;
    private String registrationDate;
    private String registrationStatus;
    private String verificationRemarks;
    private String modelNumber;
    private String creatorId;
    private String createdTime;
    private String modifiedTime;
    private String invoice;

    public Product() {
    }

    public String getRowId() { return rowId; }
    public void setRowId(String rowId) { this.rowId = rowId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getPurchaseChannel() { return purchaseChannel; }
    public void setPurchaseChannel(String purchaseChannel) { this.purchaseChannel = purchaseChannel; }

    public String getCoveragePeriod() { return coveragePeriod; }
    public void setCoveragePeriod(String coveragePeriod) { this.coveragePeriod = coveragePeriod; }

    public String getWarrantyStatus() { return warrantyStatus; }
    public void setWarrantyStatus(String warrantyStatus) { this.warrantyStatus = warrantyStatus; }

    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }

    public String getRegistrationStatus() { return registrationStatus; }
    public void setRegistrationStatus(String registrationStatus) { this.registrationStatus = registrationStatus; }

    public String getVerificationRemarks() { return verificationRemarks; }
    public void setVerificationRemarks(String verificationRemarks) { this.verificationRemarks = verificationRemarks; }

    public String getModelNumber() { return modelNumber; }
    public void setModelNumber(String modelNumber) { this.modelNumber = modelNumber; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }

    public String getModifiedTime() { return modifiedTime; }
    public void setModifiedTime(String modifiedTime) { this.modifiedTime = modifiedTime; }

    public String getInvoice() { return invoice; }
    public void setInvoice(String invoice) { this.invoice = invoice; }
}
