package com.example.demo.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.catalyst.CatalystDataStore;
import com.example.demo.dto.Product;
import com.example.demo.dto.UserRegistrationRequest;
import com.example.demo.dto.UserToZohoContactMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class ZohoCrmService {

        private final CatalystDataStore catalystDataStore;
        private final ObjectMapper objectMapper;
        private final UserToZohoContactMapper userToZohoContactMapper;
        private final ProductService productService;

        private final HttpClient httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(20))
                        .build();

        private static final String REGISTRATION_MODULE = "Luggage_Registrations";

        // Field API names written by ZohoCRMProductService.registerProductInCRM.
        private static final String REGISTRATION_FIELDS = String.join(",",
                        "id", "Name", "customerId", "productId", "Brand", "Serial_Number",
                        "Purchase_Date", "Purchase_Channel", "coveragePeriod", "warantyStatus",
                        "Registration_Date", "Registration_Status", "Verification_Remarks",
                        "Model_Number", "Invoice", "Created_By", "Created_Time", "Modified_Time");

        private final String crmApiUrl;

        public ZohoCrmService(
                        CatalystDataStore catalystDataStore,
                        ObjectMapper objectMapper,
                        @Value("${zoho.crm.api-url:https://www.zohoapis.in}") String crmApiUrl,
                        @Value("${zoho.crm.customer-field:Customer}") String customerField,
                        UserToZohoContactMapper userToZohoContactMapper,
                        ProductService productService) {

                this.catalystDataStore = catalystDataStore;
                this.objectMapper = objectMapper;
                this.userToZohoContactMapper = userToZohoContactMapper;
                this.crmApiUrl = crmApiUrl;
                this.productService = productService;
        }

        public List<Product> getCustomerProducts(Long customerId) throws Exception {

                String token = catalystDataStore.accessToken();
                String wantedCustomerId = String.valueOf(customerId);

                // The Luggage_Registrations CRM module has no customerId field, so CRM
                // cannot tell us which records belong to the customer. Catalyst does:
                // every registration is also saved there with customerId + serialNumber.
                // Use the customer's Catalyst serial numbers to pick their CRM records.
                Map<String, Product> catalystBySerial = new LinkedHashMap<>();
                try {
                        for (Product p : productService.getProductsByCustomer(customerId)) {
                                String serial = normalizeSerial(p.getSerialNumber());
                                if (serial != null) {
                                        catalystBySerial.put(serial, p);
                                }
                        }
                } catch (Exception e) {
                        System.out.println("Catalyst lookup failed for customer " + customerId
                                        + ", matching CRM by customerId only: " + e.getMessage());
                }

                List<Product> products = new ArrayList<>();
                String pageToken = null;

                do {
                        String url = crmApiUrl
                                        + "/crm/v8/" + REGISTRATION_MODULE
                                        + "?fields=" + REGISTRATION_FIELDS
                                        + "&per_page=200";
                        if (pageToken != null) {
                                url += "&page_token=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8);
                        }

                        HttpRequest request = HttpRequest.newBuilder()
                                        .uri(URI.create(url))
                                        .timeout(Duration.ofSeconds(30))
                                        .header("Authorization", "Zoho-oauthtoken " + token)
                                        .header("Accept", "application/json")
                                        .GET()
                                        .build();

                        HttpResponse<String> response = httpClient.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString());

                        // Zoho returns 204 No Content when the module has no records.
                        if (response.statusCode() == 204 || response.body() == null || response.body().isBlank()) {
                                break;
                        }

                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                                throw new RuntimeException(
                                                "Zoho CRM API failed. Status: "
                                                                + response.statusCode()
                                                                + ", Response: " + response.body());
                        }

                        JsonNode root = objectMapper.readTree(response.body());
                        JsonNode data = root.path("data");

                        if (data.isArray()) {
                                for (JsonNode record : data) {
                                        String serial = normalizeSerial(text(record, "Serial_Number"));
                                        boolean ownedByCustomer = wantedCustomerId.equals(text(record, "customerId"));
                                        Product catalyst = serial != null ? catalystBySerial.remove(serial) : null;

                                        if (ownedByCustomer || catalyst != null) {
                                                Product product = mapCrmRecordToProduct(record);
                                                fillMissingFrom(product, catalyst);
                                                if (product.getCustomerId() == null) {
                                                        product.setCustomerId(wantedCustomerId);
                                                }
                                                products.add(product);
                                        }
                                }
                        }

                        JsonNode info = root.path("info");
                        pageToken = info.path("more_records").asBoolean(false)
                                        ? text(info, "next_page_token")
                                        : null;

                } while (pageToken != null);

                int crmMatches = products.size();

                // Registrations that exist in Catalyst but were not found in CRM.
                products.addAll(catalystBySerial.values());

                System.out.println("Customer " + customerId + " products: " + products.size()
                                + " (from CRM: " + crmMatches + ", Catalyst only: " + catalystBySerial.size() + ")");

                return products;
        }

        private static String normalizeSerial(String serial) {
                return serial == null || serial.isBlank() ? null : serial.trim().toUpperCase();
        }

        /** Fills fields CRM does not store (e.g. Model_Number) from the Catalyst row. */
        private static void fillMissingFrom(Product target, Product source) {
                if (source == null) {
                        return;
                }
                if (target.getCustomerId() == null) target.setCustomerId(source.getCustomerId());
                if (target.getProductId() == null) target.setProductId(source.getProductId());
                if (target.getPurchaseDate() == null) target.setPurchaseDate(source.getPurchaseDate());
                if (target.getPurchaseChannel() == null) target.setPurchaseChannel(source.getPurchaseChannel());
                if (target.getRegistrationDate() == null) target.setRegistrationDate(source.getRegistrationDate());
                if (target.getRegistrationStatus() == null) target.setRegistrationStatus(source.getRegistrationStatus());
                if (target.getVerificationRemarks() == null) target.setVerificationRemarks(source.getVerificationRemarks());
                if (target.getModelNumber() == null) target.setModelNumber(source.getModelNumber());
                if (target.getInvoice() == null) target.setInvoice(source.getInvoice());
        }

        /** Maps a Luggage_Registrations CRM record to the API Product model. */
        private Product mapCrmRecordToProduct(JsonNode record) {
                Product product = new Product();

                product.setRowId(text(record, "id"));
                product.setCustomerId(text(record, "customerId"));
                product.setProductId(text(record, "productId"));
                product.setName(text(record, "Name"));
                product.setBrand(text(record, "Brand"));
                product.setSerialNumber(text(record, "Serial_Number"));

                String purchaseDate = text(record, "Purchase_Date");
                if (purchaseDate != null && purchaseDate.length() >= 10) {
                        try {
                                product.setPurchaseDate(LocalDate.parse(purchaseDate.substring(0, 10)));
                        } catch (DateTimeParseException ignored) {
                                // leave null if CRM returns an unexpected format
                        }
                }

                product.setPurchaseChannel(text(record, "Purchase_Channel"));
                product.setCoveragePeriod(text(record, "coveragePeriod"));
                product.setWarrantyStatus(text(record, "warantyStatus"));
                product.setRegistrationDate(text(record, "Registration_Date"));
                product.setRegistrationStatus(text(record, "Registration_Status"));
                product.setVerificationRemarks(text(record, "Verification_Remarks"));
                product.setModelNumber(text(record, "Model_Number"));
                product.setCreatorId(text(record, "Created_By"));
                product.setCreatedTime(text(record, "Created_Time"));
                product.setModifiedTime(text(record, "Modified_Time"));
                product.setInvoice(text(record, "Invoice"));

                return product;
        }

        /**
         * Reads a CRM field as text. Lookup/user fields come back as objects
         * ({"name":..,"id":..}) and file fields as arrays, so flatten those.
         */
        private String text(JsonNode record, String field) {
                JsonNode node = record.get(field);
                if (node == null || node.isNull()) {
                        return null;
                }
                if (node.isArray()) {
                        node = node.size() > 0 ? node.get(0) : null;
                        if (node == null) {
                                return null;
                        }
                }
                if (node.isObject()) {
                        if (node.hasNonNull("id")) {
                                return node.get("id").asText();
                        }
                        if (node.hasNonNull("name")) {
                                return node.get("name").asText();
                        }
                        return node.toString();
                }
                return node.asText();
        }

        public String registerUser(UserRegistrationRequest request)
                        throws Exception {

                // 2. Get Zoho access token from Catalyst
                String accessToken = catalystDataStore.accessToken();

                // 3. Convert registration details into Zoho Contact
                ObjectNode zohoContact = userToZohoContactMapper.toZohoContact(request);

                // 4. Wrap contact inside data array
                ObjectNode requestBody = objectMapper.createObjectNode();

                ArrayNode dataArray = objectMapper.createArrayNode();
                dataArray.add(zohoContact);

                requestBody.set("data", dataArray);

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                // 5. Zoho CRM API URL
                final String CRM_URL = "https://www.zohoapis.in/crm/v8/Contacts";

                // 6. Create HTTP request
                HttpRequest httpRequest = HttpRequest.newBuilder()
                                .uri(URI.create(CRM_URL))
                                .timeout(Duration.ofSeconds(30))
                                .header("Authorization",
                                                "Zoho-oauthtoken " + accessToken)
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                                .build();

                // 7. Execute HTTP request
                HttpResponse<String> response = HttpClient.newHttpClient().send(
                                httpRequest,
                                HttpResponse.BodyHandlers.ofString());

                // 8. Check response
                System.out.println("Zoho CRM Status: "
                                + response.statusCode());

                System.out.println("Zoho CRM Response: "
                                + response.body());

                if (response.statusCode() != 200
                                && response.statusCode() != 201) {

                        throw new RuntimeException(
                                        "Zoho CRM contact creation failed: "
                                                        + response.body());
                }

                // 9. Return saved user
                return "User saved in Zoho Contacts Modeule";
        }
}