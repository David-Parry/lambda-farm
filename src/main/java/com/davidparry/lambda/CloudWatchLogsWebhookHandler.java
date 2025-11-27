package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

public class CloudWatchLogsWebhookHandler implements RequestHandler<CloudWatchLogsEvent, String> {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA256_PREFIX = "sha256=";
    private static final String X_HUB_SIGNATURE = "X-Hub-Signature";
    private static final String WEBHOOK_URL = System.getenv("WEBHOOK_URL");
    private static final String SECRET_ARN = System.getenv("SECRET_ARN");
    private static final String APP_AWS_REGION = System.getenv("APP_AWS_REGION");
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Cache the secret to avoid fetching it on every invocation
    private static String cachedWebhookSecret = null;

    @Override
    public String handleRequest(CloudWatchLogsEvent event, Context context) {
        try {
            // Decode and decompress the CloudWatch Logs data
            String compressedData = event.getAwsLogs().getData();
            String decodedData = decompressLogData(compressedData);
            
            // Parse the log data
            CloudWatchLogData logData = objectMapper.readValue(decodedData, CloudWatchLogData.class);
            
            context.getLogger().log("Processing " + logData.logEvents.length + " log events from " + logData.logGroup);
            
            // Determine severity first
            String severity = determineSeverity(logData);
            context.getLogger().log("Determined severity: " + severity);
            
            // Only send to webhook if severity is high
            if ("high".equals(severity)) {
                // Build webhook payload
                ObjectNode webhookPayload = buildWebhookPayload(logData, severity);
                
                // Send to webhook
                sendToWebhook(webhookPayload, context);
                context.getLogger().log("Payload sent successfully payload is:"+objectMapper.writeValueAsString(webhookPayload));
                return "Successfully processed " + logData.logEvents.length + " log events with high severity - webhook sent";
            } else {
                return "Processed " + logData.logEvents.length + " log events with " + severity + " severity - webhook not sent";
            }
            
        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            throw new RuntimeException("Failed to process CloudWatch Logs event", e);
        }
    }

    private String decompressLogData(String compressedData) throws IOException {
        byte[] compressedBytes = Base64.getDecoder().decode(compressedData);
        
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedBytes);
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        
        byte[] buffer = new byte[1024];
        int len;
        while ((len = gzipInputStream.read(buffer)) > 0) {
            byteArrayOutputStream.write(buffer, 0, len);
        }
        
        gzipInputStream.close();
        byteArrayOutputStream.close();
        
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    }

    private ObjectNode buildWebhookPayload(CloudWatchLogData logData, String severity) {
        ObjectNode payload = objectMapper.createObjectNode();
        
        payload.put("alert_type", "cloudwatch_logs");
        payload.put("severity", severity);
        payload.put("log_group", logData.logGroup);
        payload.put("log_stream", logData.logStream);
        payload.put("event_count", logData.logEvents.length);
        payload.put("source", "AWS CloudWatch Logs");
        payload.put("timestamp", Instant.now().toString());
        payload.put("git_repo_uri","git@github.com:David-Parry/lambda-farm.git");
        payload.put("jira_project_key","SCRUM");
        // Add log events
        ArrayNode eventsArray = payload.putArray("log_events");
        for (LogEvent logEvent : logData.logEvents) {
            ObjectNode eventNode = eventsArray.addObject();
            eventNode.put("id", logEvent.id);
            eventNode.put("timestamp", logEvent.timestamp);
            eventNode.put("message", logEvent.message);
            
            // Add formatted timestamp
            eventNode.put("timestamp_formatted", 
                Instant.ofEpochMilli(logEvent.timestamp).toString());
        }
        
        return payload;
    }

    private String determineSeverity(CloudWatchLogData logData) {
        // Check if any log messages contain error indicators
        for (LogEvent event : logData.logEvents) {
            String message = event.message.toLowerCase();
            if (message.contains("error") || message.contains("exception") || 
                message.contains("fatal") || message.contains("critical")) {
                return "high";
            }
        }
        return "medium";
    }

    /**
     * Fetches the webhook secret from AWS Secrets Manager
     * Uses caching to avoid fetching on every invocation
     */
    private String getWebhookSecret(Context context) {
        if (cachedWebhookSecret != null) {
            return cachedWebhookSecret;
        }
        
        if (SECRET_ARN == null || SECRET_ARN.isEmpty()) {
            context.getLogger().log("WARNING: SECRET_ARN not configured, webhook signature will not be added");
            return null;
        }
        
        try {
            Region region = APP_AWS_REGION != null ? Region.of(APP_AWS_REGION) : Region.US_EAST_2;
            
            try (SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(region)
                    .build()) {
                
                GetSecretValueRequest request = GetSecretValueRequest.builder()
                        .secretId(SECRET_ARN)
                        .build();
                
                GetSecretValueResponse response = client.getSecretValue(request);
                cachedWebhookSecret = response.secretString();
                context.getLogger().log("Successfully fetched webhook secret from Secrets Manager");
                return cachedWebhookSecret;
            }
        } catch (Exception e) {
            context.getLogger().log("ERROR: Failed to fetch secret from Secrets Manager: " + e.getMessage());
            return null;
        }
    }
    
    private void sendToWebhook(ObjectNode payload, Context context) throws IOException, InterruptedException {
        if (WEBHOOK_URL == null || WEBHOOK_URL.isEmpty()) {
            throw new RuntimeException("WEBHOOK_URL environment variable is not set");
        }

        String jsonPayload = objectMapper.writeValueAsString(payload);
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(WEBHOOK_URL))
                .header("Content-Type", "application/json")
                .header("User-Agent", "AWS-Lambda-CloudWatch-Logs-Forwarder")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));
        
        // Fetch webhook secret from Secrets Manager and add signature
        String webhookSecret = getWebhookSecret(context);
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            try {
                String signature = generateSignature(jsonPayload, webhookSecret);
                requestBuilder.header(X_HUB_SIGNATURE, signature);
                context.getLogger().log("Added X-Hub-Signature header");
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                context.getLogger().log("WARNING: Failed to generate signature: " + e.getMessage());
            }
        }
        
        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        context.getLogger().log("Webhook response status: " + response.statusCode());
        context.getLogger().log("Webhook response body: " + response.body());
        
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Webhook returned non-success status: " + response.statusCode());
        }
    }

    /**
     * Generates HMAC-SHA256 signature for the given payload and secret
     *
     * @param payload The payload to sign
     * @param secret The secret key
     * @return The signature in format "sha256=<hex>"
     * @throws NoSuchAlgorithmException if HMAC-SHA256 is not available
     * @throws InvalidKeyException if the secret key is invalid
     */
    public String generateSignature(String payload, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
        );
        mac.init(secretKey);

        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return SHA256_PREFIX + bytesToHex(hash);
    }

    /**
     * Converts byte array to hexadecimal string
     *
     * @param bytes The byte array to convert
     * @return Hexadecimal string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // Inner classes for JSON deserialization
    public static class CloudWatchLogData {
        public String messageType;
        public String owner;
        public String logGroup;
        public String logStream;
        public String[] subscriptionFilters;
        public LogEvent[] logEvents;
    }

    public static class LogEvent {
        public String id;
        public long timestamp;
        public String message;
    }
}
