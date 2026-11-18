package com.example.demo.service;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.example.demo.catalyst.CatalystDataStore;
import com.example.demo.dto.Product;
import com.example.demo.dto.ProductRegistrationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class ZohoCRMProductService {

    private final CatalystDataStore catalystDataStore;
    private final ObjectMapper objectMapper;
     private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String CRM_URL =
            "https://www.zohoapis.in/crm/v8/Luggage_Registrations";

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

    public ZohoCRMProductService(
            CatalystDataStore catalystDataStore,
            ObjectMapper objectMapper) {

        this.catalystDataStore = catalystDataStore;
        this.objectMapper = objectMapper;
    }

    private void putIfNotNull(
        ObjectNode object,
        String fieldName,
        String value) {

    if (value != null && !value.isBlank()) {
        object.put(fieldName, value);
    }
}
    public String registerProductInCRM(
        ProductRegistrationRequest request,
        Long customerId) throws Exception {

    String accessToken = catalystDataStore.accessToken();

    ObjectNode crmProduct = objectMapper.createObjectNode();
        

    // Mandatory CRM field
    crmProduct.put("Name", request.getModelNumber());

        String serialNumber =
                request.getSerialNumber().trim().toUpperCase();

    // Same fields as Catalyst DataStore
    crmProduct.put("customerId", String.valueOf(customerId));

    crmProduct.put("Brand",
            request.getBrand());
    crmProduct.put("Serial_Number",
            serialNumber);;

    crmProduct.put("Purchase_Date",
            request.getPurchaseDate().toString());

            crmProduct.put("coveragePeriod",
            request.getCoveragePeriod());

    crmProduct.put("Purchase_Channel",
            request.getPurchaseChannel().trim());

    crmProduct.put("Registration_Date",
            LocalDateTime.now().format(DATE_TIME_FORMAT));

    crmProduct.put("warantyStatus",
            request.getWarantyStatus().trim());
    crmProduct.put("productId",
            request.getProductId().trim());

    crmProduct.put("Registration_Status", "REGISTERED");

    if (request.getModelNumber() != null &&
            !request.getModelNumber().isBlank()) {


        crmProduct.put("Model_Number",
                request.getModelNumber().trim());
    }

    if (request.getVerificationRemarks() != null &&
            !request.getVerificationRemarks().isBlank()) {

        crmProduct.put("Verification_Remarks",
                request.getVerificationRemarks().trim());
    }

    if (request.getInvoice() != null &&
            !request.getInvoice().isBlank()) {

        crmProduct.put("Invoice",
                request.getInvoice().trim());
    }

    // Prepare CRM payload
    ArrayNode data = objectMapper.createArrayNode();
    data.add(crmProduct);

    ObjectNode payload = objectMapper.createObjectNode();
    payload.set("data", data);

    String requestBody =
            objectMapper.writeValueAsString(payload);

    System.out.println("Zoho CRM Request: " + requestBody);

    HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(
                    "https://www.zohoapis.in/crm/v8/Luggage_Registrations"))
            .header("Authorization",
                    "Zoho-oauthtoken " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

    HttpResponse<String> response =
            httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() < 200 ||
            response.statusCode() >= 300) {

        throw new RuntimeException(
                "Zoho CRM API Failed: " + response.body());
    }

    return response.body();
}
}
