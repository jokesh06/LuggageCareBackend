package com.example.demo.service;

import com.example.demo.catalyst.CatalystContext;
import com.example.demo.dto.Product;
import com.example.demo.dto.ProductRegistrationRequest;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;
import com.zc.component.object.ZCRowPagedResponse;
import com.zc.component.object.ZCTable;
import com.zc.component.zcql.ZCQL;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    // Catalyst table name
    private static final String TABLE_NAME = "customerProducts";

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Get Catalyst table using table name with explicit CatalystContext.admin()
    private ZCTable getTable() throws Exception {
        return ZCObject.getInstance(CatalystContext.admin()).getTable(TABLE_NAME);
    }

    // =====================================================
    // 1. REGISTER PRODUCT
    // =====================================================

    public Product registerProduct(
            Long customerId,
            ProductRegistrationRequest request) throws Exception {

        validateRegistration(customerId, request);

        String serialNumber =
                request.getSerialNumber().trim().toUpperCase();

        // Check duplicate serial number using explicit context
        String duplicateQuery =
                "SELECT ROWID FROM " + TABLE_NAME +
                " WHERE serialNumber = '" +
                escapeSql(serialNumber) + "'";

        List<ZCRowObject> duplicateRows =
                ZCQL.getInstance(CatalystContext.admin()).executeQuery(duplicateQuery);

        if (duplicateRows != null && !duplicateRows.isEmpty()) {
            throw new IllegalArgumentException(
                    "This serial number is already registered");
        }

        // Create Catalyst row
        ZCRowObject row = ZCRowObject.getInstance();

        row.set("customerId", customerId);
        row.set("serialNumber", serialNumber);
        row.set("purchaseDate", request.getPurchaseDate().toString());
        row.set("purchaseChannel", request.getPurchaseChannel().trim());

        row.set(
                "registrationDate",
                LocalDateTime.now().format(DATE_TIME_FORMAT)
        );

        row.set("registrationStatus", "REGISTERED");

        if (request.getModelNumber() != null &&
                !request.getModelNumber().isBlank()) {

            row.set("modelNumber", request.getModelNumber().trim());
        }

        if (request.getVerificationRemarks() != null &&
                !request.getVerificationRemarks().isBlank()) {

            row.set(
                    "verificationRemarks",
                    request.getVerificationRemarks().trim()
            );
        }

        if (request.getInvoice() != null &&
                !request.getInvoice().isBlank()) {

            row.set(
                    "invoice",
                    request.getInvoice().trim()
            );
        }
        System.out.println("Inserting Product Row"+ row);
        // Insert product into Catalyst Data Store
        ZCRowObject savedRow = getTable().insertRow(row);

        return convertToProduct(savedRow);
    }

    // =====================================================
    // 2. GET PRODUCTS BY CUSTOMER ID
    // =====================================================

    public List<Product> getProductsByCustomer(Long customerId) throws Exception {

        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        String query =
                "SELECT * FROM " + TABLE_NAME +
                " WHERE customerId = " + customerId;

        List<ZCRowObject> rows =
                ZCQL.getInstance(CatalystContext.admin()).executeQuery(query);

        List<Product> products = new ArrayList<>();

        if (rows != null) {
            for (ZCRowObject row : rows) {
                products.add(convertToProduct(row));
            }
        }

        return products;
    }

    // =====================================================
    // 3. GET PRODUCT BY PRODUCT ROWID
    // =====================================================

    public Product getProductById(Long productId) throws Exception {

        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        ZCRowObject row = getTable().getRow(productId);

        if (row == null) {
            throw new IllegalArgumentException("Product not found");
        }

        return convertToProduct(row);
    }

    // =====================================================
    // 4. GET ALL PRODUCTS
    // =====================================================

    public List<Product> getAllProducts() throws Exception {

        List<Product> products = new ArrayList<>();

        String nextToken = null;
        ZCRowPagedResponse page;

        do {
            page = getTable().getPagedRows(nextToken, 200);

            if (page.getRows() != null) {
                for (ZCRowObject row : page.getRows()) {
                    products.add(convertToProduct(row));
                }
            }

            nextToken = page.getNextToken();

        } while (page.moreRecordsAvailable());

        return products;
    }

    // =====================================================
    // 5. UPDATE REGISTRATION STATUS
    // =====================================================

    public Product updateRegistrationStatus(
            Long productId,
            String registrationStatus) throws Exception {

        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        if (registrationStatus == null ||
                registrationStatus.isBlank()) {

            throw new IllegalArgumentException(
                    "Registration status is required");
        }

        // Check product exists
        ZCRowObject existingRow = getTable().getRow(productId);

        if (existingRow == null) {
            throw new IllegalArgumentException("Product not found");
        }

        ZCRowObject updateRow = ZCRowObject.getInstance();

        updateRow.set("ROWID", productId);

        updateRow.set(
                "registrationStatus",
                registrationStatus.trim().toUpperCase()
        );

        List<ZCRowObject> rowsToUpdate = new ArrayList<>();
        rowsToUpdate.add(updateRow);

        getTable().updateRows(rowsToUpdate);

        return getProductById(productId);
    }

    // =====================================================
    // 6. DELETE PRODUCT
    // =====================================================

    public void deleteProduct(Long productId) throws Exception {

        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Invalid product ID");
        }

        ZCRowObject existingRow = getTable().getRow(productId);

        if (existingRow == null) {
            throw new IllegalArgumentException("Product not found");
        }

        getTable().deleteRow(productId);
    }

    // =====================================================
    // 7. CONVERT CATALYST ROW TO PRODUCT OBJECT
    // =====================================================

    private Product convertToProduct(ZCRowObject row) {

        Product product = new Product();

        product.setRowId(getString(row, "ROWID"));
        product.setCustomerId(getString(row, "customerId"));
        product.setCreatorId(getString(row, "CREATORID"));

        product.setSerialNumber(
                getString(row, "serialNumber"));

        String purchaseDate = getString(row, "purchaseDate");

        if (purchaseDate != null && !purchaseDate.isBlank()) {
            product.setPurchaseDate(
                    LocalDate.parse(purchaseDate.substring(0, 10))
            );
        }

        product.setPurchaseChannel(
                getString(row, "purchaseChannel"));

        product.setRegistrationDate(
                getString(row, "registrationDate"));

        product.setRegistrationStatus(
                getString(row, "registrationStatus"));

        product.setVerificationRemarks(
                getString(row, "verificationRemarks"));

        product.setModelNumber(
                getString(row, "modelNumber"));

        product.setCreatedTime(
                getString(row, "CREATEDTIME"));

        product.setModifiedTime(
                getString(row, "MODIFIEDTIME"));

        return product;
    }

    // =====================================================
    // 8. VALIDATE REGISTRATION
    // =====================================================

    private void validateRegistration(
            Long customerId,
            ProductRegistrationRequest request) {

        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Product details are required");
        }

        if (request.getSerialNumber() == null ||
                request.getSerialNumber().isBlank()) {

            throw new IllegalArgumentException(
                    "Serial number is required");
        }

        if (request.getPurchaseDate() == null) {
            throw new IllegalArgumentException(
                    "Purchase date is required");
        }

        if (request.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Purchase date cannot be in the future");
        }

        if (request.getPurchaseChannel() == null ||
                request.getPurchaseChannel().isBlank()) {

            throw new IllegalArgumentException(
                    "Purchase channel is required");
        }
    }

    // =====================================================
    // 9. GET STRING VALUE FROM CATALYST ROW
    // =====================================================

    private String getString(ZCRowObject row, String columnName) {

        Object value = row.get(columnName);

        return value == null ? null : value.toString();
    }

    // =====================================================
    // 10. ESCAPE SQL VALUE
    // =====================================================

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }

    // =====================================================
    // 11. GET CUSTOMER PRODUCTS
    // =====================================================

    public List<Product> getCustomerProducts(Long customerId) throws Exception {

        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }

        String query = "SELECT * FROM " + TABLE_NAME + " WHERE customerId = "
                + customerId;

        List<ZCRowObject> rows = ZCQL.getInstance(CatalystContext.admin())
                .executeQuery(query);

        List<Product> products = new ArrayList<>();

        if (rows != null) {
            for (ZCRowObject row : rows) {
                products.add(convertToProduct(row));
            }
        }

        return products;
    }
}