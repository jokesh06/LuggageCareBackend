package com.example.demo.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Sends push notifications through Firebase Cloud Messaging (FCM).
 *
 * Each customer's phone subscribes to the topic "customer_<customerId>" after login
 * (Flutter: FirebaseMessaging.instance.subscribeToTopic('customer_$customerId')),
 * so no device tokens need to be stored on the backend.
 *
 * Credentials (Firebase Console > Project settings > Service accounts > Generate new private key):
 *   FIREBASE_SERVICE_ACCOUNT_JSON = the whole JSON file content, or
 *   FIREBASE_SERVICE_ACCOUNT_PATH = path to the JSON file.
 * Firebase is set up on first use, so the app still starts when these are missing.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private static final String APP_NAME = "luggage-care-push";

    private final String serviceAccountJson;
    private final String serviceAccountPath;

    private volatile FirebaseMessaging messaging;

    public PushNotificationService(
            @Value("${firebase.service-account-json:}") String serviceAccountJson,
            @Value("${firebase.service-account-path:}") String serviceAccountPath) {
        this.serviceAccountJson = serviceAccountJson == null ? "" : serviceAccountJson.trim();
        this.serviceAccountPath = serviceAccountPath == null ? "" : serviceAccountPath.trim();
    }

    /** Topic a customer's devices subscribe to. */
    public static String customerTopic(String customerId) {
        return "customer_" + customerId;
    }

    /**
     * Sends a notification to every device subscribed to the customer's topic.
     * @return the FCM message ID
     */
    public String sendToCustomer(String customerId, String title, String body, Map<String, String> data)
            throws Exception {

        Message message = Message.builder()
                .setTopic(customerTopic(customerId))
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder().setSound("default").build())
                        .build())
                .build();

        String messageId = messaging().send(message);
        log.info("Push sent to topic {} (messageId={})", customerTopic(customerId), messageId);
        return messageId;
    }

    private FirebaseMessaging messaging() throws Exception {
        FirebaseMessaging current = messaging;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (messaging == null) {
                FirebaseApp app = FirebaseApp.getApps().stream()
                        .filter(a -> APP_NAME.equals(a.getName()))
                        .findFirst()
                        .orElse(null);
                if (app == null) {
                    try (InputStream credentials = openCredentials()) {
                        FirebaseOptions options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(credentials))
                                .build();
                        app = FirebaseApp.initializeApp(options, APP_NAME);
                    }
                }
                messaging = FirebaseMessaging.getInstance(app);
            }
            return messaging;
        }
    }

    private InputStream openCredentials() throws Exception {
        if (!serviceAccountJson.isBlank()) {
            return new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        }
        if (!serviceAccountPath.isBlank()) {
            return new FileInputStream(serviceAccountPath);
        }
        throw new IllegalStateException(
                "Firebase is not configured. Set FIREBASE_SERVICE_ACCOUNT_JSON "
                        + "or FIREBASE_SERVICE_ACCOUNT_PATH.");
    }
}
