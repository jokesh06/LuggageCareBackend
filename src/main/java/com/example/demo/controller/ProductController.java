package com.example.demo.controller;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.Product;
import com.example.demo.dto.ProductRegistrationRequest;
import com.example.demo.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // 1. Register product for a customer
    @PostMapping("/customer/{customerId}")
    public ResponseEntity<Product> registerProduct(
            @PathVariable Long customerId,
            @RequestBody ProductRegistrationRequest request) throws Exception {

        Product product =
                productService.registerProduct(customerId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(product);
    }

    // 2. Get all products of a particular customer
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Product>> getCustomerProducts(
            @PathVariable Long customerId) throws Exception {

        return ResponseEntity.ok(
                productService.getCustomerProducts(customerId));
    }

    // 3. Get all registered products (admin use)
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() throws Exception {

        return ResponseEntity.ok(
                productService.getAllProducts());
    }

    // 4. Get individual product
    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProductById(
            @PathVariable Long productId) throws Exception {

        return ResponseEntity.ok(
                productService.getProductById(productId));
    }
}
