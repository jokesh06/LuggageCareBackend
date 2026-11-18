
package com.example.demo.catalyst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CatalystDataStore {

    private static final Logger log = LoggerFactory.getLogger(CatalystDataStore.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final String configurationMessage;
    private final String accountsUrl;

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;

    private volatile String accessToken;
    private volatile long accessTokenExpiresAt;

    public CatalystDataStore(
            @Value("${catalyst.project.domain:https://api.catalyst.zoho.in}") String projectDomain,
            @Value("${catalyst.environment:Development}") String environment,
            @Value("${catalyst.accounts-url:https://accounts.zoho.in}") String accountsUrl,
            @Value("${catalyst.client-id:}") String clientId,
            @Value("${catalyst.client-secret:}") String clientSecret,
            @Value("${catalyst.refresh-token:}") String refreshToken) {

        this.accountsUrl = trimTrailingSlash(blankToEmpty(accountsUrl));
        this.clientId = blankToEmpty(clientId);
        this.clientSecret = blankToEmpty(clientSecret);
        this.refreshToken = blankToEmpty(refreshToken);
       
        this.configurationMessage = missingCredentialMessage();
    } 

    public String accessToken() throws Exception {

        long now = System.currentTimeMillis();

        String current = accessToken;

        if (current != null && now < accessTokenExpiresAt) {
            return current;
        }

        synchronized (this) {

            now = System.currentTimeMillis();

            if (accessToken != null && now < accessTokenExpiresAt) {
                return accessToken;
            }

            if (clientId.isBlank()
                    || clientSecret.isBlank()
                    || refreshToken.isBlank()) {

                throw new IllegalStateException(configurationMessage);
            }

            Map<String, String> form = new LinkedHashMap<>();

            form.put("refresh_token", refreshToken);
            form.put("client_id", clientId);
            form.put("client_secret", clientSecret);
            form.put("grant_type", "refresh_token");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(accountsUrl + "/oauth/v2/token"))
                    .timeout(Duration.ofSeconds(30))
                    .header(
                            "Content-Type",
                            "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            encodeForm(form)))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            JsonNode json = objectMapper.readTree(
                    response.body() == null ? "{}" : response.body());

            if (response.statusCode() < 200
                    || response.statusCode() >= 300
                    || json.has("error")) {

                String reason = json.path("error").asText("");

                if (reason.isBlank()) {
                    reason = safeBody(response.body());
                }

                log.error("Zoho OAuth refresh failed: {}", reason);

                throw new IllegalStateException(
                        "Zoho OAuth refresh failed for "
                                + accountsUrl + ": " + reason
                                + ". Check your Client ID, Client Secret, "
                                + "Refresh Token and Zoho India API Console.");
            }

            String token = json.path("access_token").asText("");

            if (token.isBlank()) {
                throw new IllegalStateException(
                        "Zoho OAuth refresh did not return an access token");
            }

            long expiresInSeconds = json.path("expires_in").asLong(3600L);

            accessToken = token;

            accessTokenExpiresAt = System.currentTimeMillis()
                    + Math.max(30L, expiresInSeconds - 60L) * 1000L;

            return token;
        }
    }

    private String missingCredentialMessage() {

        return "Local Catalyst Data Store credentials are missing. "
                + "Create a Self Client in "
                + "https://api-console.zoho.in using the Zoho account "
                + "that owns your Catalyst project. Configure "
                + "CATALYST_CLIENT_ID, CATALYST_CLIENT_SECRET, "
                + "and CATALYST_REFRESH_TOKEN. "
                + "Required scopes: "
                + "ZohoCatalyst.zcql.CREATE,"
                + "ZohoCatalyst.tables.rows.READ,"
                + "ZohoCatalyst.tables.rows.CREATE,"
                + "ZohoCatalyst.tables.rows.UPDATE,"
                + "ZohoCatalyst.tables.rows.DELETE. "
                + "Confirm CATALYST_PROJECT_ID in Catalyst Console. "
                + "India API domain: https://api.catalyst.zoho.in";
    }

    private static String encodeForm(Map<String, String> fields) {

        return fields.entrySet().stream()
                .map(entry -> URLEncoder.encode(
                        entry.getKey(),
                        StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(
                                entry.getValue(),
                                StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static String safeBody(String body) {

        if (body == null || body.isBlank()) {
            return "empty response";
        }

        String compact = body.replaceAll("\\s+", " ").trim();

        return compact.length() > 500
                ? compact.substring(0, 500)
                : compact;
    }

    private static String blankToEmpty(String value) {

        return value == null ? "" : value.trim();
    }

    private static String trimTrailingSlash(String value) {

        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}
