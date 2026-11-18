package com.example.demo.controller;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.Product;
import com.example.demo.dto.ProductRegistrationRequest;
import com.example.demo.service.ProductService;
import com.example.demo.service.ZohoCRMProductService;
import com.example.demo.service.ZohoCrmService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ZohoCrmService zohoCrmService;
    private final ZohoCRMProductService zohoCRMProductService;

    public ProductController(ProductService productService, ZohoCrmService zohoCrmService, ZohoCRMProductService zohoCRMProductService) {
        this.productService = productService;
        this.zohoCrmService = zohoCrmService;
        this.zohoCRMProductService = zohoCRMProductService;
    }

    // 1. Register product for a customer
    @PostMapping("/customer/{customerId}")
    public ResponseEntity<String> registerProduct(
            @PathVariable Long customerId,
            @RequestBody ProductRegistrationRequest request) throws Exception {

         Product product =
                productService.registerProduct(customerId, request);


            String crmResponse =
                    zohoCRMProductService.registerProductInCRM(
                            request,customerId
                    );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(crmResponse);
    }

    // 2. Get all products of a particular customer
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Product>> getCustomerProducts(
            @PathVariable Long customerId) throws Exception {                                
        return ResponseEntity.ok(
                zohoCrmService.getCustomerProducts(customerId));
    }

    // 3. Get all registered products (admin use)
//    @GetMapping()
//     public ResponseEntity<List<Product>> getAllProducts() throws Exception {

//         return ResponseEntity.ok(
//                productService.getCustomerProducts( @PathVariable Long customerId));
//     }

    // 4. Get individual product
    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProductById(
            @PathVariable Long productId) throws Exception {

        return ResponseEntity.ok(
                productService.getProductById(productId));
    }
}
