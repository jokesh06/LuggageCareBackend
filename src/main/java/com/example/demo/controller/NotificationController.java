package com.example.demo.controller;

import com.example.demo.dto.ClaimStatusNotificationRequest;
import com.example.demo.service.NotificationLogService;
import com.example.demo.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Webhook called by Zoho Creator when a claim's status changes.
 *
 * POST /api/notifications/claim-status
 * Header: X-Webhook-Secret: <NOTIFICATION_WEBHOOK_SECRET>
 * Body:   see ClaimStatusNotificationRequest
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final PushNotificationService pushNotificationService;
    private final NotificationLogService notificationLogService;
    private final String webhookSecret;

    public NotificationController(
            PushNotificationService pushNotificationService,
            NotificationLogService notificationLogService,
            @Value("${notification.webhook-secret:}") String webhookSecret) {
        this.pushNotificationService = pushNotificationService;
        this.notificationLogService = notificationLogService;
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
    }

    @PostMapping("/claim-status")
    public ResponseEntity<Map<String, Object>> claimStatusChanged(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @Validated @RequestBody ClaimStatusNotificationRequest request) {

        String topic = PushNotificationService.customerTopic(request.getCustomerId());

        if (!isValidSecret(secret)) {
            notificationLogService.record(request.getCustomerId(), request.getClaimId(), request.getStatus(),
                    null, null, topic, false, null, "Rejected: invalid or missing webhook secret");
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or missing webhook secret"));
        }

        String status = request.getStatus().trim();
        String claimId = request.getClaimId().trim();

        String title = isBlank(request.getTitle())
                ? "Claim " + claimId + " updated"
                : request.getTitle().trim();
        String body = isBlank(request.getMessage())
                ? "Your claim status is now: " + status
                : request.getMessage().trim();

        // Extra data the Flutter app can use to open the right claim when tapped
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", "claim_status");
        data.put("claimId", claimId);
        data.put("stageName", status);
        data.put("customerId", request.getCustomerId());

        try {
            String messageId = pushNotificationService.sendToCustomer(
                    request.getCustomerId(), title, body, data);

            notificationLogService.record(request.getCustomerId(), claimId, status,
                    title, body, topic, true, messageId, null);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("messageId", messageId);
            result.put("topic", topic);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Push notification failed for claim {}", claimId, e);
            notificationLogService.record(request.getCustomerId(), claimId, status,
                    title, body, topic, false, null, String.valueOf(e.getMessage()));
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", String.valueOf(e.getMessage())));
        }
    }

    /** Missing or wrong fields in the body: answer 400 with the field errors (instead of a generic 500). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid request", "fields", fields));
    }

    /** Body is not valid JSON. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Request body must be valid JSON"));
    }

    /** Rejects every call when no secret is configured, so the endpoint is never left open. */
    private boolean isValidSecret(String provided) {
        if (webhookSecret.isBlank() || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                provided.trim().getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
