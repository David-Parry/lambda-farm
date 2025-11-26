package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class CloudWatchLogsWebhookHandlerTest {

    private CloudWatchLogsWebhookHandler handler;
    private Context mockContext;

    @BeforeEach
    void setUp() {

        handler = new CloudWatchLogsWebhookHandler();
        
        // Create mock Context
        mockContext = new Context() {
            @Override
            public String getAwsRequestId() {
                return "test-request-id";
            }

            @Override
            public String getLogGroupName() {
                return "/aws/lambda/test-function";
            }

            @Override
            public String getLogStreamName() {
                return "2024/01/01/[$LATEST]test-stream";
            }

            @Override
            public String getFunctionName() {
                return "test-function";
            }

            @Override
            public String getFunctionVersion() {
                return "$LATEST";
            }

            @Override
            public String getInvokedFunctionArn() {
                return "arn:aws:lambda:us-east-1:123456789012:function:test-function";
            }

            @Override
            public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                return 30000;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 512;
            }

            @Override
            public LambdaLogger getLogger() {
                return new LambdaLogger() {
                    @Override
                    public void log(String message) {
                        System.out.println("LAMBDA LOG: " + message);
                    }

                    @Override
                    public void log(byte[] message) {
                        System.out.println("LAMBDA LOG: " + new String(message, StandardCharsets.UTF_8));
                    }
                };
            }
        };
    }

    @Test
    void handleRequest() throws Exception {
        // Create a CloudWatchLogsEvent with sample log data
        CloudWatchLogsEvent event = new CloudWatchLogsEvent();
        
        // Create sample log data
        Map<String, Object> logData = new HashMap<>();
        logData.put("messageType", "DATA_MESSAGE");
        logData.put("owner", "123456789012");
        logData.put("logGroup", "/aws/lambda/test-function");
        logData.put("logStream", "2024/01/01/[$LATEST]test-stream");
        logData.put("subscriptionFilters", new String[]{"test-filter"});
        
        // Create log events with error message to trigger high severity
        Map<String, Object>[] logEvents = new Map[2];
        logEvents[0] = new HashMap<>();
        logEvents[0].put("id", "event-1");
        logEvents[0].put("timestamp", System.currentTimeMillis());
        logEvents[0].put("message", "ERROR: Something went wrong in the application");
        
        logEvents[1] = new HashMap<>();
        logEvents[1].put("id", "event-2");
        logEvents[1].put("timestamp", System.currentTimeMillis());
        logEvents[1].put("message", "Exception occurred: NullPointerException");
        
        logData.put("logEvents", logEvents);
        
        // Convert to JSON and compress
        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(logData);
        String compressedData = compressAndEncode(jsonData);
        
        // Create and set the AWSLogs data
        CloudWatchLogsEvent.AWSLogs awsLogs = new CloudWatchLogsEvent.AWSLogs();
        awsLogs.setData(compressedData);
        event.setAwsLogs(awsLogs);
        
        // Execute the handler
        String result = handler.handleRequest(event, mockContext);
        
        // Verify no null pointer exceptions occurred
        assertNotNull(result);
        System.out.println("Handler result: " + result);
    }

    @Test
    void generateSignature() throws Exception {
        String payload = "{\"test\":\"data\"}";
        String secret = "test-secret";
        
        String signature = handler.generateSignature(payload, secret);
        
        assertNotNull(signature);
        assertTrue(signature.startsWith("sha256="));
        System.out.println("Generated signature: " + signature);
    }
    
    /**
     * Compress and Base64 encode the log data (simulating CloudWatch Logs format)
     */
    private String compressAndEncode(String data) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
        gzipOutputStream.close();
        
        byte[] compressed = byteArrayOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(compressed);
    }

}