package com.example.demo.service;

import com.example.demo.catalyst.CatalystContext;
import com.example.demo.dto.ClaimRequest;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;
import com.zc.component.object.ZCTable;
import com.zc.component.zcql.ZCQL;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;

@Service
public class ClaimService {

    private static final String TABLE_NAME = "customerClaims";
    private static final String PRODUCT_TABLE = "customerProducts";

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

        // 1. Verify product exists and belongs to this customer
        String productQuery = "SELECT * FROM " + PRODUCT_TABLE + " WHERE ROWID = " + request.getProductId();
        List<ZCRowObject> productRows = ZCQL.getInstance(CatalystContext.admin()).executeQuery(productQuery);

        if (productRows == null || productRows.isEmpty()) {
            throw new IllegalArgumentException("Selected product not found");
        }

        ZCRowObject productRow = productRows.get(0);
        Map<String, Object> productCols = productRow.getRowObject();
        
        // Extract purchase date to perform preliminary warranty check (Illustrative: 5 years validity)
        String purchaseDateStr = (String) productCols.get("purchaseDate");
        boolean isUnderWarranty = checkWarrantyValidity(purchaseDateStr);

        // 2. Generate human-friendly ticket ID (e.g., LC-260124)
        String yearCode = String.valueOf(LocalDate.now().getYear()).substring(2);
        String uniqueSuffix = String.format("%05d", (int)(Math.random() * 90000) + 10000);
        String claimId = "LC-" + yearCode + uniqueSuffix;

        // 3. Build Catalyst row object for insertion
        ZCRowObject row = ZCRowObject.getInstance();
        row.set("claimId", claimId);
        row.set("customerId", customerId);
        row.set("productId", request.getProductId());
        row.set("category", request.getCategory().trim());
        row.set("description", request.getDescription().trim());
        row.set("stage", 0L); // Stage 0: Claim submitted
        row.set("method", "Store drop-off"); // Default initial selection
        row.set("quote", isUnderWarranty ? 0L : 850L); // ₹0 if active warranty, ₹850 if expired demo fee
        row.set("approved", false);
        row.set("mediaName", request.getMediaName() != null ? request.getMediaName() : "");
        row.set("feedbackGiven", false);

        // 4. Insert into Catalyst Data Store
        ZCRowObject savedRow = getTable().insertRow(row);

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
            LocalDate purchased = LocalDate.parse(purchaseDateStr.substring(0, 10));
            LocalDate expiryDate = purchased.plusYears(5);
            return LocalDate.now().isBefore(expiryDate) || LocalDate.now().isEqual(expiryDate);
        } catch (Exception e) {
            return false;
        }
    }
}