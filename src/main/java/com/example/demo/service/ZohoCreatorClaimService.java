package com.example.demo.service;

import com.example.demo.catalyst.CatalystDataStore;
import com.example.demo.dto.ClaimRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ZohoCreatorClaimService {

    private final ObjectMapper objectMapper;
    private final CatalystDataStore catalystDataStore;

    private String creatorUrl="https://creator.zoho.in/api/v2/demo9fristinetech/luggage-care-operations-system/form/service";

    public ZohoCreatorClaimService(
            ObjectMapper objectMapper,
            CatalystDataStore catalystDataStore) {

        this.objectMapper = objectMapper;
        this.catalystDataStore = catalystDataStore;
    }

   public void createClaimInZohoCreator(
        ClaimRequest request,
        Long customerId,
        String claimId,
        boolean isUnderWarranty) throws Exception {

    String accessToken = catalystDataStore.accessToken();

    ObjectNode root = objectMapper.createObjectNode();
    ObjectNode data = objectMapper.createObjectNode();

    data.put("claim_id", claimId);
    data.put("customer_id", String.valueOf(customerId));
    data.put("product_id", request.getProductId());

    data.put("request_status", "Claim submitted");

    ArrayNode luggageRegistration = objectMapper.createArrayNode();
    luggageRegistration.add(request.getProductId());
    data.set("luggage_registration", luggageRegistration);

    data.put("issue_description", request.getDescription().trim());
    data.put("Catalyst_ID", String.valueOf(customerId));
    data.put("City", "Pune");
    data.put("issue_category", request.getCategory().trim());

    data.put("submitted_on",
            LocalDate.now().format(
                    DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)));

    data.put("service_method",
            request.getMethod() != null ? request.getMethod() : "Store drop-off");

    data.put("luggage_service_request_name", claimId);

    ArrayNode customer = objectMapper.createArrayNode();
    customer.add(String.valueOf(customerId));
    data.set("customer", customer);

    data.put("stage", 0);
    data.put("quote", isUnderWarranty ? 0 : 850);
    data.put("approved", false);

    data.put("mediaName",
            request.getMediaName() != null ? request.getMediaName() : "");

    data.put("feedbackGiven", false);

    root.set("data", data);

    String jsonBody = objectMapper.writeValueAsString(root);

    HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(creatorUrl))
            .header("Authorization", "Zoho-oauthtoken " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                    jsonBody, StandardCharsets.UTF_8))
            .build();

    HttpResponse<String> response = HttpClient.newHttpClient().send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new RuntimeException(
                "Zoho Creator failed. Status: "
                        + response.statusCode()
                        + ", Response: " + response.body());
    }

    System.out.println("Zoho Creator response: " + response.body());
}
}