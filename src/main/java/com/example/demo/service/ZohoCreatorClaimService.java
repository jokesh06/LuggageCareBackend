package com.example.demo.service;

import com.example.demo.catalyst.CatalystDataStore;
import com.example.demo.dto.ClaimRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    data.put("request_status", request.resolveStage());

    ArrayNode luggageRegistration = objectMapper.createArrayNode();
    luggageRegistration.add(request.getProductId());
    data.set("luggage_registration", luggageRegistration);

    data.put("issue_description", request.getDescription().trim());
    data.put("Catalyst_ID", String.valueOf(customerId));
    data.put("City", request.resolveCenter());
    data.put("issue_category", request.getCategory().trim());

    data.put("submitted_on",
            LocalDate.now().format(
                    DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)));

    data.put("service_method", request.resolveMethod());

    data.put("luggage_service_request_name", claimId);

    ArrayNode customer = objectMapper.createArrayNode();
    customer.add(String.valueOf(customerId));
    data.set("customer", customer);

    data.put("stage", request.resolveStage());
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

    // Report (list view) of the "service" form in Zoho Creator.
    // Set zoho.creator.claims-report in application properties if your report link name differs.
    @Value("${zoho.creator.claims-report:service_Report}")
    private String claimsReport;

    private static final String CREATOR_REPORT_BASE =
            "https://creator.zoho.in/api/v2/demo9fristinetech/luggage-care-operations-system/report/";

    /** Open claims for a customer from Zoho Creator (stage is anything except "Closed"). */
    public List<Map<String, Object>> getOpenClaimsFromCreator(Long customerId) throws Exception {
        return fetchClaimsFromCreator(customerId, false);
    }

    /** Closed claims for a custofmer from Zoho Creator (stage is "Closed"). */
    public List<Map<String, Object>> getClosedClaimsFromCreator(Long customerId) throws Exception {
        return fetchClaimsFromCreator(customerId, true);
    }

    private List<Map<String, Object>> fetchClaimsFromCreator(Long customerId, boolean closed) throws Exception {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        String accessToken = catalystDataStore.accessToken();

        // Catalyst_ID is a TEXT field in Creator, so the value must be in quotes
        String criteria = "(Catalyst_ID == \"" + customerId + "\""
                + " && request_status " + (closed ? "==" : "!=")
                + " \"" + ClaimQueryService.CLOSED_STAGE + "\")";

        String url = CREATOR_REPORT_BASE + claimsReport
                + "?max_records=1000&criteria="
                + URLEncoder.encode(criteria, StandardCharsets.UTF_8);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Zoho-oauthtoken " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Creator GET URL: " + url);
        System.out.println("Creator GET Status: " + response.statusCode());
        System.out.println("Creator GET Response: " + response.body());

        JsonNode body = response.body() == null || response.body().isBlank()
                ? objectMapper.createObjectNode()
                : objectMapper.readTree(response.body());

        // Creator returns code 9280 ("No records found") when nothing matches
        if (body.path("code").asInt() == 9280) {
            return new ArrayList<>();
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Zoho Creator fetch failed. Status: "
                            + response.statusCode()
                            + ", Response: " + response.body());
        }

        JsonNode data = body.path("data");
        List<Map<String, Object>> claims = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode record : data) {
                claims.add(toFlutterClaim(record, customerId));
            }
        }
        return claims;
    }

    /** Stage shown when Creator has no request_status for a claim. */
    private static final String DEFAULT_STAGE = "Claim submitted";

    /**
     * Maps a Zoho Creator record to the same keys the Catalyst Data Store returned,
     * so the Flutter app keeps working without changes.
     */
    private Map<String, Object> toFlutterClaim(JsonNode r, Long customerId) {
        String stageText = text(r, "request_status");
        String productId = firstNonBlank(text(r, "product_id"), text(r, "luggage_registration"));
        String quote = text(r, "quote");

        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("ROWID", text(r, "ID"));                                   // Creator record ID
        m.put("claimId", text(r, "luggage_service_request_name"));
        m.put("customerId", customerId);
        m.put("productId", productId);
        m.put("category", text(r, "issue_category"));
        m.put("description", text(r, "issue_description"));
        m.put("stageName", stageText.isBlank() ? DEFAULT_STAGE : stageText);
        m.put("CREATEDTIME", firstNonBlank(text(r, "Added_Time"), text(r, "submitted_on")));
        m.put("method", firstNonBlank(text(r, "service_method"), "Store drop-off"));
        m.put("center", text(r, "City"));
        m.put("quote", quote.isBlank() ? 0.0 : parseDouble(quote));
        m.put("approved", Boolean.parseBoolean(text(r, "approved")));
        m.put("mediaName", firstNonBlank(text(r, "mediaName"), text(r, "File_upload")));
        m.put("feedbackGiven", Boolean.parseBoolean(text(r, "feedbackGiven")));
        return m;
    }

    /** Reads a Creator field as text; handles plain values, lookups ({display_value}) and lists. */
    private String text(JsonNode r, String field) {
        JsonNode v = r.get(field);
        if (v == null || v.isNull()) return "";
        if (v.isArray()) v = v.size() > 0 ? v.get(0) : null;
        if (v == null || v.isNull()) return "";
        if (v.isObject()) {
            JsonNode dv = v.has("display_value") ? v.get("display_value") : v.get("ID");
            return dv == null ? "" : dv.asText("");
        }
        return v.asText("");
    }

    private String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b == null ? "" : b);
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}