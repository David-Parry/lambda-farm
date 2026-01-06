package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Additional unit tests for CloudWatchLogsWebhookHandler
 * Tests handleRequest method and data processing logic
 * Note: Tests that require WEBHOOK_URL environment variable will verify behavior without it
 */
@ExtendWith(MockitoExtension.class)
class CloudWatchLogsWebhookHandlerFullTest {
    
    private CloudWatchLogsWebhookHandler handler;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private LambdaLogger mockLogger;
    
    @BeforeEach
    void setUp() {
        handler = new CloudWatchLogsWebhookHandler();
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }
    
    /**
     * Helper method to compress and encode log data
     */
    private String compressAndEncode(String data) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
        gzipOutputStream.close();
        
        byte[] compressedBytes = byteArrayOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(compressedBytes);
    }
    
    /**
     * Helper method to create CloudWatch Logs event
     */
    private CloudWatchLogsEvent createCloudWatchEvent(String compressedData) {
        CloudWatchLogsEvent event = new CloudWatchLogsEvent();
        CloudWatchLogsEvent.AWSLogs awsLogs = new CloudWatchLogsEvent.AWSLogs();
        awsLogs.setData(compressedData);
        event.setAwsLogs(awsLogs);
        return event;
    }
    
    /**
     * Helper to create valid log data JSON
     */
    private String createLogDataJson(String... messages) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"messageType\":\"DATA_MESSAGE\",");
        json.append("\"owner\":\"123456789012\",");
        json.append("\"logGroup\":\"/aws/lambda/test-function\",");
        json.append("\"logStream\":\"2024/01/01/[$LATEST]abcd1234\",");
        json.append("\"subscriptionFilters\":[\"test-filter\"],");
        json.append("\"logEvents\":[");
        
        for (int i = 0; i < messages.length; i++) {
            if (i > 0) json.append(",");
            json.append("{");
            json.append("\"id\":\"").append(i).append("\",");
            json.append("\"timestamp\":").append(System.currentTimeMillis()).append(",");
            json.append("\"message\":\"").append(messages[i]).append("\"");
            json.append("}");
        }
        
        json.append("]");
        json.append("}");
        return json.toString();
    }
    
    @Test
    void shouldThrowExceptionForHighSeverityWithoutWebhookUrl() throws Exception {
        // Arrange
        String logData = createLogDataJson("ERROR: Something went wrong");
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act & Assert - Should throw exception due to missing WEBHOOK_URL for high severity
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            handler.handleRequest(event, mockContext);
        });
        
        // Verify the exception is about missing WEBHOOK_URL
        assertTrue(exception.getMessage().contains("WEBHOOK_URL") || 
                   exception.getCause() != null);
        
        // Verify logging occurred
        verify(mockLogger, atLeastOnce()).log(anyString());
    }
    
    @Test
    void shouldThrowExceptionForExceptionMessagesWithoutWebhookUrl() throws Exception {
        // Arrange
        String logData = createLogDataJson("NullPointerException occurred");
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            handler.handleRequest(event, mockContext);
        });
        
        // Verify exception or cause exists
        assertNotNull(exception);
        verify(mockLogger, atLeastOnce()).log(anyString());
    }
    
    @Test
    void shouldReturnSuccessForMediumSeverity() throws Exception {
        // Arrange
        String logData = createLogDataJson("INFO: Application started successfully");
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act
        String result = handler.handleRequest(event, mockContext);
        
        // Assert - Medium severity should not trigger webhook
        assertNotNull(result);
        assertTrue(result.contains("medium") || result.contains("webhook not sent"));
        verify(mockLogger, atLeastOnce()).log(anyString());
    }
    
    @Test
    void shouldHandleMultipleLogEvents() throws Exception {
        // Arrange
        String logData = createLogDataJson(
            "INFO: First message",
            "INFO: Second message",
            "INFO: Third message"
        );
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act
        String result = handler.handleRequest(event, mockContext);
        
        // Assert
        assertNotNull(result);
        verify(mockLogger).log(contains("3 log events"));
    }
    
    @Test
    void shouldDecompressAndParseLogData() throws Exception {
        // Arrange
        String logData = createLogDataJson("Test message");
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act
        String result = handler.handleRequest(event, mockContext);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Processed") || result.contains("log events"));
    }
    
    @Test
    void shouldHandleInvalidCompressedData() {
        // Arrange
        CloudWatchLogsEvent event = createCloudWatchEvent("invalid-base64-data!!!");
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            handler.handleRequest(event, mockContext);
        });
    }
    
    @Test
    void shouldLogSeverityDetermination() throws Exception {
        // Arrange
        String logData = createLogDataJson("ERROR: Critical failure");
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act
        try {
            handler.handleRequest(event, mockContext);
        } catch (RuntimeException e) {
            // Expected - no webhook URL
        }
        
        // Assert
        verify(mockLogger).log(contains("Determined severity"));
    }
    
    @Test
    void shouldThrowForFatalMessagesWithoutWebhook() throws Exception {
        // Arrange
        String logData = createLogDataJson("FATAL: System crash");
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act & Assert - Fatal should be high severity and require webhook
        assertThrows(RuntimeException.class, () -> {
            handler.handleRequest(event, mockContext);
        });
    }
    
    @Test
    void shouldThrowForCriticalMessagesWithoutWebhook() throws Exception {
        // Arrange
        String logData = createLogDataJson("CRITICAL: Database connection lost");
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            handler.handleRequest(event, mockContext);
        });
    }
    
    @Test
    void shouldHandleEmptyLogEvents() throws Exception {
        // Arrange
        String logData = "{\"messageType\":\"DATA_MESSAGE\",\"owner\":\"123456789012\",\"logGroup\":\"/aws/lambda/test\",\"logStream\":\"stream\",\"subscriptionFilters\":[],\"logEvents\":[]}";
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act
        String result = handler.handleRequest(event, mockContext);
        
        // Assert
        assertNotNull(result);
        verify(mockLogger).log(contains("0 log events"));
    }
    
    @Test
    void shouldThrowForMixedSeverityWithError() throws Exception {
        // Arrange
        String logData = createLogDataJson(
            "INFO: Normal operation",
            "ERROR: Something bad happened",
            "INFO: Recovery attempted"
        );
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act & Assert - Should be high severity due to ERROR
        assertThrows(RuntimeException.class, () -> {
            handler.handleRequest(event, mockContext);
        });
    }
    
    @Test
    void shouldHandleLogDataWithSpecialCharacters() throws Exception {
        // Arrange
        String logData = createLogDataJson("INFO: User @john.doe logged in with special chars: $%^&*");
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act
        String result = handler.handleRequest(event, mockContext);
        
        // Assert
        assertNotNull(result);
    }
    
    @Test
    void shouldHandleLogDataWithUnicode() throws Exception {
        // Arrange
        String logData = createLogDataJson("INFO: User 世界 accessed the system 🌍");
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act
        String result = handler.handleRequest(event, mockContext);
        
        // Assert
        assertNotNull(result);
    }
    
    @Test
    void shouldLogProcessingCount() throws Exception {
        // Arrange
        String logData = createLogDataJson("INFO: Test 1", "INFO: Test 2");
        String compressedData = compressAndEncode(logData);
        CloudWatchLogsEvent event = createCloudWatchEvent(compressedData);
        
        // Act
        handler.handleRequest(event, mockContext);
        
        // Assert
        verify(mockLogger).log(contains("Processing 2 log events"));
    }
}
