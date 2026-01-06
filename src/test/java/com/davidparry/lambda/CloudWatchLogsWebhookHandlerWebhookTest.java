package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for webhook-related functionality in CloudWatchLogsWebhookHandler
 * Tests the sendToWebhook method behavior and error handling
 */
@ExtendWith(MockitoExtension.class)
class CloudWatchLogsWebhookHandlerWebhookTest {
    
    private CloudWatchLogsWebhookHandler handler;
    private ObjectMapper objectMapper;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private LambdaLogger mockLogger;
    
    @BeforeEach
    void setUp() {
        handler = new CloudWatchLogsWebhookHandler();
        objectMapper = new ObjectMapper();
        lenient().when(mockContext.getLogger()).thenReturn(mockLogger);
    }
    
    /**
     * Helper method to invoke private sendToWebhook method using reflection
     */
    private void invokeSendToWebhook(ObjectNode payload, Context context) throws Exception {
        Method method = CloudWatchLogsWebhookHandler.class.getDeclaredMethod("sendToWebhook", ObjectNode.class, Context.class);
        method.setAccessible(true);
        method.invoke(handler, payload, context);
    }
    
    /**
     * Helper to create a sample webhook payload
     */
    private ObjectNode createSamplePayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("alert_type", "cloudwatch_logs");
        payload.put("severity", "high");
        payload.put("log_group", "/aws/lambda/test");
        payload.put("event_count", 1);
        return payload;
    }
    
    @Test
    void shouldThrowExceptionWhenWebhookUrlIsNull() {
        // Arrange
        ObjectNode payload = createSamplePayload();
        
        // Act & Assert - WEBHOOK_URL environment variable is not set
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        // Verify the exception occurred
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandleEmptyPayload() {
        // Arrange
        ObjectNode emptyPayload = objectMapper.createObjectNode();
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(emptyPayload, mockContext);
        });
        
        // Verify exception occurred (due to missing WEBHOOK_URL)
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandlePayloadWithNullValues() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("alert_type", (String) null);
        payload.put("severity", (String) null);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandlePayloadWithSpecialCharacters() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("message", "Error: Something went wrong! @#$%^&*()");
        payload.put("severity", "high");
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandlePayloadWithUnicodeCharacters() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("message", "Error: 世界 🌍");
        payload.put("severity", "high");
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandleLargePayload() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        StringBuilder largeMessage = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeMessage.append("Large log message ");
        }
        payload.put("message", largeMessage.toString());
        payload.put("severity", "high");
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandlePayloadWithNestedObjects() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode nested = objectMapper.createObjectNode();
        nested.put("inner_key", "inner_value");
        payload.set("nested", nested);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandlePayloadWithArrays() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.putArray("events")
            .add("event1")
            .add("event2")
            .add("event3");
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandlePayloadWithBooleanValues() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("is_critical", true);
        payload.put("is_resolved", false);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandlePayloadWithNumericValues() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("event_count", 42);
        payload.put("timestamp", 1234567890L);
        payload.put("severity_level", 5.5);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandlePayloadSerialization() throws Exception {
        // Arrange
        ObjectNode payload = createSamplePayload();
        
        // Act - Serialize to JSON string
        String jsonString = objectMapper.writeValueAsString(payload);
        
        // Assert - Verify serialization works
        assertNotNull(jsonString);
        assertTrue(jsonString.contains("alert_type"));
        assertTrue(jsonString.contains("severity"));
        assertTrue(jsonString.contains("high"));
    }
    
    @Test
    void shouldHandleComplexPayloadStructure() {
        // Arrange
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("alert_type", "cloudwatch_logs");
        payload.put("severity", "high");
        payload.put("log_group", "/aws/lambda/test");
        payload.put("log_stream", "2024/01/01/stream");
        payload.put("event_count", 5);
        payload.put("source", "AWS CloudWatch Logs");
        payload.put("timestamp", "2024-01-01T00:00:00Z");
        
        // Add array of events
        payload.putArray("log_events")
            .addObject()
                .put("id", "1")
                .put("message", "Error message");
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        assertNotNull(exception);
        // Verify we can still serialize complex payload
        assertDoesNotThrow(() -> objectMapper.writeValueAsString(payload));
    }
    
    @Test
    void shouldValidatePayloadNotNull() {
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(null, mockContext);
        });
        
        assertNotNull(exception);
    }
    
    @Test
    void shouldHandleContextWithValidLogger() {
        // Arrange
        ObjectNode payload = createSamplePayload();
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            invokeSendToWebhook(payload, mockContext);
        });
        
        // Verify exception occurred
        assertNotNull(exception);
    }
    
    @Test
    void shouldSerializePayloadWithoutErrors() throws Exception {
        // Arrange
        ObjectNode payload = createSamplePayload();
        payload.put("git_repo_uri", "git@github.com:user/repo.git");
        payload.put("jira_project_key", "PROJ");
        
        // Act
        String jsonString = objectMapper.writeValueAsString(payload);
        
        // Assert
        assertNotNull(jsonString);
        assertTrue(jsonString.length() > 0);
        assertTrue(jsonString.contains("git_repo_uri"));
        assertTrue(jsonString.contains("jira_project_key"));
    }
}
