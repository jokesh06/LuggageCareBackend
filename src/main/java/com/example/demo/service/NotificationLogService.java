package com.example.demo.service;

import com.example.demo.catalyst.CatalystContext;
import com.example.demo.catalyst.CatalystDataStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Saves every push-notification attempt to the Catalyst Data Store table "notificationLogs"
 * so failures can be checked later (Catalyst Console > Data Store > notificationLogs).
 *
 * How the row is written:
 *   - Request came through Catalyst (deployed / catalyst serve): Catalyst Java SDK.
 *   - Otherwise (e.g. VS Code debug on localhost): Catalyst REST API with the Zoho OAuth token
 *     from CatalystDataStore (needs scope ZohoCatalyst.tables.rows.CREATE).
 *
 * Logging never breaks the notification: if the write fails, the reason is written to the
 * server log and the request carries on.
 *
 * Table "notificationLogs" columns:
 *   customerId varchar, claimId varchar, status varchar, title varchar, message text,
 *   topic varchar, success boolean, messageId varchar, errorMessage text, source varchar
 */
@Service
public class NotificationLogService {

    private static final Logger log = LoggerFactory.getLogger(NotificationLogService.class);
    private static final String TABLE_NAME = "notificationLogs";

    private final CatalystDataStore catalystDataStore;
    private final ObjectMapper objectMapper;
    private final String projectDomain;
    private final String projectId;
    private final String environment;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public NotificationLogService(
            CatalystDataStore catalystDataStore,
            ObjectMapper objectMapper,
            @Value("${catalyst.project.domain:https://api.catalyst.zoho.in}") String projectDomain,
            @Value("${notification.log.project-id:87676000000059001}") String projectId,
            @Value("${catalyst.environment:Development}") String environment) {
        this.catalystDataStore = catalystDataStore;
        this.objectMapper = objectMapper;
        this.projectDomain = projectDomain.endsWith("/")
                ? projectDomain.substring(0, projectDomain.length() - 1)
                : projectDomain;
        this.projectId = projectId.trim();
        this.environment = environment.trim();
    }

    public void record(String customerId, String claimId, String status, String title, String message,
                       String topic, boolean success, String messageId, String errorMessage) {

        // Always leave a line in the server log as well
        log.info("PUSH_LOG customerId={} claimId={} status={} topic={} success={} messageId={} error={}",
                customerId, claimId, status, topic, success, messageId, errorMessage);

        String sCustomerId = limit(customerId, 255);
        String sClaimId = limit(claimId, 255);
        String sStatus = limit(status, 255);
        String sTitle = limit(title, 255);
        String sMessage = limit(message, 2000);
        String sTopic = limit(topic, 255);
        String sMessageId = limit(messageId, 255);
        String sError = limit(errorMessage, 2000);

        try {
            if (hasCatalystContext()) {
                ZCRowObject row = ZCRowObject.getInstance();
                row.set("customerId", sCustomerId);
                row.set("claimId", sClaimId);
                row.set("status", sStatus);
                row.set("title", sTitle);
                row.set("message", sMessage);
                row.set("topic", sTopic);
                row.set("success", success);
                row.set("messageId", sMessageId);
                row.set("errorMessage", sError);
                row.set("source", "zoho-creator");
                ZCObject.getInstance(CatalystContext.admin()).getTable(TABLE_NAME).insertRow(row);
            } else {
                ObjectNode row = objectMapper.createObjectNode();
                row.put("customerId", sCustomerId);
                row.put("claimId", sClaimId);
                row.put("status", sStatus);
                row.put("title", sTitle);
                row.put("message", sMessage);
                row.put("topic", sTopic);
                row.put("success", success);
                row.put("messageId", sMessageId);
                row.put("errorMessage", sError);
                row.put("source", "zoho-creator");
                insertViaRestApi(row);
            }
            log.debug("Push log saved to {}", TABLE_NAME);
        } catch (Exception e) {
            log.warn("Could not save push log to Data Store table {}: {}", TABLE_NAME, e.getMessage());
        }
    }

    private boolean hasCatalystContext() {
        try {
            CatalystContext.admin();
            return true;
        } catch (IllegalStateException notThroughCatalyst) {
            return false;
        }
    }

    private void insertViaRestApi(ObjectNode row) throws Exception {
        ArrayNode rows = objectMapper.createArrayNode();
        rows.add(row);

        String url = projectDomain + "/baas/v1/project/" + projectId + "/table/" + TABLE_NAME + "/row";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Zoho-oauthtoken " + catalystDataStore.accessToken())
                .header("Environment", environment)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(rows.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Catalyst REST insert failed. Status: "
                    + response.statusCode() + ", Response: " + response.body());
        }
    }

    private static String limit(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }
}
