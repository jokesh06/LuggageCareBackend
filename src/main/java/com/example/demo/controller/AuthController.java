
package com.example.demo.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.CatalystAuthProvider;
import com.zc.auth.CatalystSDK;
import com.zc.component.ZCUserDetail;
import com.zc.component.users.ZCUser;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {

        try {
            String incomingProjectId = request.getHeader("X-ZC-ProjectId");
            System.out.print("incomingProjectId" + incomingProjectId);
            System.out.println(
                    "PROJECT ID MATCH: " +
                            "87676000000059001".equals(incomingProjectId));

            System.out.println(
                    "CATALYST ENVIRONMENT: " +
                            request.getHeader("X-ZC-Environment"));
            // STEP 1: Initialize Catalyst SDK with current request
            CatalystSDK.init(new CatalystAuthProvider(request));

            System.out.println("Catalyst SDK initialized");

            // STEP 2: Check request details (never print secret values)
            System.out.println("Request URI: " + request.getRequestURI());

            String projectSecret = request.getHeader("X-ZC-PROJECT-SECRET-KEY");

            System.out.println(
                    "Project secret header present: "
                            + (projectSecret != null && !projectSecret.isBlank()));

            // STEP 3: Retrieve authenticated Catalyst user
            ZCUserDetail user = ZCUser.getInstance().getCurrentUser();

            if (user == null) {

                return ResponseEntity.status(401).body(
                        Map.of(
                                "status", "FAILED",
                                "message", "User is not authenticated"));
            }

            // STEP 4: Print user information
            System.out.println("Catalyst user retrieved successfully");
            System.out.println("Status: " + user.getStatus());
            System.out.println("Role Details: " + user.getRoleDetails());

            // STEP 5: Prepare response
            Map<String, Object> response = new LinkedHashMap<>();

            response.put("status", "SUCCESS");
            response.put(
                    "message",
                    "Authenticated user retrieved");
            response.put("user", user);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            Map<String, Object> error = new LinkedHashMap<>();

            error.put("status", "FAILED");
            error.put(
                    "message",
                    "Unable to retrieve authenticated user");
            error.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(error);
        }
    }
}
