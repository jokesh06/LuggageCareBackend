
package com.example.demo.dto;

import java.time.LocalDate;

public class ProductRegistrationRequest {

    private String serialNumber;
    private LocalDate purchaseDate;
    private String purchaseChannel;
    private String modelNumber;
    private String verificationRemarks;
    private String invoice;


    public String getInvoice() {
        return invoice;
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
}
