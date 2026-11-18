
package com.example.demo.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;

public class ProductRegistrationRequest {

    private String serialNumber;
    private LocalDate purchaseDate;
    private String purchaseChannel;
    private String modelNumber;
    private String verificationRemarks;
    private String invoice;
    private String brand;
    private String  coveragePeriod;
    private String warantyStatus;
    private String productId;

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getBrand() {
        return brand;
    }

    public ProductRegistrationRequest() {
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getPurchaseChannel() {
        return purchaseChannel;
    }

    public void setPurchaseChannel(String purchaseChannel) {
        this.purchaseChannel = purchaseChannel;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getVerificationRemarks() {
        return verificationRemarks;
    }

    public void setVerificationRemarks(String verificationRemarks) {
        this.verificationRemarks = verificationRemarks;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
    }
    public String getInvoice() {
        return invoice;
    }

    public JsonNode isFeedbackGiven() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isFeedbackGiven'");
    }

    public String getCoveragePeriod() {
        return coveragePeriod;
    }

    public void setCoveragePeriod(String coveragePeriod) {
        this.coveragePeriod = coveragePeriod;
    }

    public String getWarantyStatus() {
        return warantyStatus;
    }

    public void setWarantyStatus(String warantyStatus) {
        this.warantyStatus = warantyStatus;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    
}
