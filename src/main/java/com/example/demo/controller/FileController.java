package com.example.demo.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.zc.component.stratus.ZCBucket;
import com.zc.component.stratus.ZCStratus;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class FileController {

    // Replace with your exact Stratus bucket name from the Catalyst console
    private static final String BUCKET_NAME = "luggagedocuments"; 

    // Your Stratus public base URL for viewing files in the UI
    private static final String STRATUS_BASE_URL = "https://luggagedocuments-development.zohostratus.in"; // Update with your Stratus domain/URL

    @PostMapping("/upload")
    public ResponseEntity uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty"));
            }

            String originalFileName = file.getOriginalFilename();
            // Optional: prepend a timestamp to keep filenames unique in the bucket
            String objectKey = System.currentTimeMillis() + "_" + originalFileName.replaceAll("\\s+", "_");

            // 1. Get the Stratus Bucket instance
            ZCBucket bucket = ZCStratus.getInstance().bucketInstance(BUCKET_NAME);

            // 2. Upload file stream directly to your Stratus bucket
            try (InputStream inputStream = file.getInputStream()) {
                boolean isUploaded = bucket.putObject(objectKey, inputStream);
                
                if (!isUploaded) {
                    return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file to Stratus bucket."));
                }
            }

            // 3. Construct the public file URL for your React UI
            String fileUrl = STRATUS_BASE_URL + "/" + objectKey;

            return ResponseEntity.ok(Map.of(
                    "fileName", originalFileName,
                    "fileUrl", fileUrl
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Stratus upload failed: " + e.getMessage()));
        }
    }
}