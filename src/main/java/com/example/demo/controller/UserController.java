
package com.example.demo.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.CatalystAuthProvider;
import com.zc.auth.CatalystSDK;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;
import com.zc.component.object.ZCRowPagedResponse;
import com.zc.component.object.ZCTable;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String TABLE_NAME = "users";

    @GetMapping
    public ResponseEntity<?> getAllUsers(HttpServletRequest request) {

        try {
            // Initialize Catalyst SDK for the current request
            CatalystSDK.init(new CatalystAuthProvider(request));

            // Get Catalyst Data Store instance
            ZCObject object = ZCObject.getInstance();

            // Get users table using table name
            ZCTable table = object.getTable(TABLE_NAME);

            List<Map<String, Object>> users = new ArrayList<>();

            String nextToken = null;
            ZCRowPagedResponse pagedResponse;

            // Retrieve all records using pagination
            do {
                pagedResponse = table.getPagedRows(nextToken, 200);

                for (ZCRowObject row : pagedResponse.getRows()) {

                    Map<String, Object> user = new LinkedHashMap<>();

                    user.put("ROWID", row.get("ROWID"));
                    user.put("CREATORID", row.get("CREATORID"));
                    user.put("CREATEDTIME", row.get("CREATEDTIME"));
                    user.put("MODIFIEDTIME", row.get("MODIFIEDTIME"));

                    user.put("authUserId", row.get("authUserId"));
                    user.put("firstName", row.get("firstName"));
                    user.put("lastName", row.get("lastName"));
                    user.put("email", row.get("email"));
                    user.put("mobileNumber", row.get("mobileNumber"));
                    user.put("profileImage", row.get("profileImage"));
                    user.put("accountStatus", row.get("accountStatus"));
                    user.put("isEmailVerified", row.get("isEmailVerified"));
                    user.put("lastLoginAt", row.get("lastLoginAt"));

                    users.add(user);
                }

                if (pagedResponse.moreRecordsAvailable()) {
                    nextToken = pagedResponse.getNextToken();
                }

            } while (pagedResponse.moreRecordsAvailable());

            Map<String, Object> response = new LinkedHashMap<>();

            response.put("status", "SUCCESS");
            response.put("totalUsers", users.size());
            response.put("users", users);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            Map<String, Object> error = new LinkedHashMap<>();

            error.put("status", "FAILED");
            error.put("message", "Unable to retrieve users from Catalyst");
            error.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(error);
        }
    }
}
