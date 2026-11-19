package com.example.demo.service;

import com.example.demo.catalyst.CatalystContext;
import com.example.demo.catalyst.CatalystDataStore;
import com.example.demo.dto.ClaimRequest;
import com.example.demo.dto.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;
import com.zc.component.object.ZCTable;
import com.zc.component.zcql.ZCQL;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ClaimService {

    private static final String TABLE_NAME = "customerClaims";
    private static final String PRODUCT_TABLE = "customerProducts";
    private final ObjectMapper objectMapper;
    private final CatalystDataStore catalystDataStore;
    private final ZohoCrmService zohoCrmService;

    public ClaimService(
            ObjectMapper objectMapper,
            CatalystDataStore catalystDataStore,
            ZohoCrmService zohoCrmService) {

        this.objectMapper = objectMapper;
        this.catalystDataStore = catalystDataStore;
        this.zohoCrmService = zohoCrmService;
    }

    private ZCTable getTable() throws Exception {
        return ZCObject.getInstance(CatalystContext.admin()).getTable(TABLE_NAME);
    }

    /**
     * Creates a new service claim ticket for a customer's product.
     */
    public Map<String, Object> raiseClaim(Long customerId, ClaimRequest request) throws Exception {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        // 1. Verify product exists
        // --- OLD: validate productId from Catalyst Data Store ---
        // String productQuery =
        // "SELECT * FROM " + PRODUCT_TABLE +
        // " WHERE productId = '" + escapeSql( request.getProductId()) + "'";
        // List<ZCRowObject> productRows = ZCQL.getInstance(CatalystContext.admin()).executeQuery(productQuery);
        //
        // if (productRows == null || productRows.isEmpty()) {
        //     throw new IllegalArgumentException("Selected product not found");
        // }
        //
        // ZCRowObject productRow = productRows.get(0);
        // Map<String, Object> productCols = productRow.getRowObject();
        //
        // // Extract purchase date to perform preliminary warranty check (Illustrative: 5 years validity)
        // String purchaseDateStr = (String) productCols.get("purchaseDate");
        // boolean isUnderWarranty = checkWarrantyValidity(purchaseDateStr);

        // --- NEW: validate productId from Zoho CRM (Luggage_Registrations module) ---
        Product crmProduct = zohoCrmService.findProductByProductId(request.getProductId());
        if (crmProduct == null) {
            throw new IllegalArgumentException("Selected product not found");
        }

        // Extract purchase date to perform preliminary warranty check (Illustrative: 5 years validity)
        String purchaseDateStr = crmProduct.getPurchaseDate() != null
                ? crmProduct.getPurchaseDate().toString()
                : null;
        boolean isUnderWarranty = checkWarrantyValidity(purchaseDateStr);

        // 2. Use the ticket ID sent by the app; otherwise generate one (e.g., LC-2612345)
        String claimId = request.getClaimId();
        if (claimId == null || claimId.isBlank()) {
            String yearCode = String.valueOf(LocalDate.now().getYear()).substring(2);
            String uniqueSuffix = String.format("%05d", (int)(Math.random() * 90000) + 10000);
            claimId = "LC-" + yearCode + uniqueSuffix;
        } else {
            claimId = claimId.trim();
        }

        // 3. Build Catalyst row object for insertion
        ZCRowObject row = ZCRowObject.getInstance();
        row.set("claimId", claimId);
        row.set("customerId", customerId);
        row.set("productId", request.getProductId());
        row.set("category", request.getCategory().trim());
        row.set("description", request.getDescription().trim());
        row.set("stage", request.resolveStage()); // Actual stage text from client, default "Claim submitted"
        row.set("method", request.resolveMethod()); // Service method chosen in the app
        row.set("quote", isUnderWarranty ? 0L : 850L); // ₹0 if active warranty, ₹850 if expired demo fee
        row.set("approved", false);
        row.set("mediaName", request.getMediaName() != null ? request.getMediaName() : "");
        row.set("feedbackGiven", false);

        // 4. Insert into Catalyst Data Store
        ZCRowObject savedRow = getTable().insertRow(row);
        // 5. Push claim data to Zoho Creator
    pushClaimToZohoCreator(
            customerId,
            request,
            claimId,
            isUnderWarranty
    );

        return savedRow.getRowObject();
    }

    /**
     * Helper to check if warranty is still active (5-year rule simulation matching frontend).
     */
    private boolean checkWarrantyValidity(String purchaseDateStr) {
        if (purchaseDateStr == null || purchaseDateStr.isBlank()) {
            return false;
        }
        try {
            LocalDate purchased;
            try {
                // ISO format from Catalyst (yyyy-MM-dd...)
                purchased = LocalDate.parse(purchaseDateStr.substring(0, 10));
            } catch (Exception isoEx) {
                // Zoho Creator date format (dd-MMM-yyyy, e.g. 19-Nov-2026)
                purchased = LocalDate.parse(purchaseDateStr.trim().substring(0, 11),
                        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH));
            }
            LocalDate expiryDate = purchased.plusYears(5);
            return LocalDate.now().isBefore(expiryDate) || LocalDate.now().isEqual(expiryDate);
        } catch (Exception e) {
            return false;
        }

    }
    private String escapeSql(String value) {
    return value.replace("'", "''");
}


private void pushClaimToZohoCreator(
        Long customerId,
        ClaimRequest request,
        String claimId,
        boolean isUnderWarranty) throws Exception {

    String accessToken = catalystDataStore.accessToken();

    ObjectNode claimData = objectMapper.createObjectNode();

    // Claim ID from Catalyst
    claimData.put("Catalyst_ID", customerId);

    // Claim form name
    claimData.put(
            "luggage_service_request_name",
            claimId
    );

    // Claim status
    claimData.put(
            "request_status",
            request.resolveStage()
    );
    claimData.put(
            "File_upload",
            request.getMediaName() != null ? request.getMediaName() : ""
    );

    // Claim description
    claimData.put(
            "issue_description",
            request.getDescription().trim()
    );
    // Service center selected in the app goes to the City field
    claimData.put(
            "City",
            request.resolveCenter()
    );
    // Issue category
    claimData.put(
            "issue_category",
            request.getCategory().trim()
    );

    // Service method
    claimData.put(
            "service_method",
            request.resolveMethod()
    );

    // Submitted date
    claimData.put(
            "submitted_on",
            LocalDate.now().format(
                    DateTimeFormatter.ofPattern("dd-MMM-yyyy")
            )
    );

    // Create API request body
    ObjectNode requestBody = objectMapper.createObjectNode();
    requestBody.set("data", claimData);

    String creatorUrl =
            "https://creator.zoho.in/api/v2/" +
            "demo9fristinetech/" +
            "luggage-care-operations-system/" +
            "form/service";

    HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(creatorUrl))
            .header(
                    "Authorization",
                    "Zoho-oauthtoken " + accessToken
            )
            .header(
                    "Content-Type",
                    "application/json"
            )
            .POST(
                    HttpRequest.BodyPublishers.ofString(
                            requestBody.toString()
                    )
            )
            .build();

    HttpResponse<String> response =
            HttpClient.newHttpClient().send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

    System.out.println("Creator Claim Request: "
            + requestBody);

    System.out.println("Creator Claim Response: "
            + response.body());

    if (response.statusCode() < 200 ||
            response.statusCode() >= 300) {

        throw new RuntimeException(
                "Zoho Creator API failed. Status: "
                + response.statusCode()
                + ", Response: "
                + response.body()
        );
    }
}
}