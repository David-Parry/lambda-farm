package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Enhanced unit tests for CloudWatchLogsWebhookHandler
 * Focuses on testing getWebhookSecret() and related functionality
 * Uses reflection to test private methods and reset static fields
 */
@ExtendWith(MockitoExtension.class)
class CloudWatchLogsWebhookHandlerEnhancedTest {
    
    private CloudWatchLogsWebhookHandler handler;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private LambdaLogger mockLogger;
    
    @BeforeEach
    void setUp() throws Exception {
        handler = new CloudWatchLogsWebhookHandler();
        lenient().when(mockContext.getLogger()).thenReturn(mockLogger);
        
        // Reset cached secret using reflection
        resetCachedSecret();
    }
    
    /**
     * Helper method to reset the cached webhook secret using reflection
     */
    private void resetCachedSecret() throws Exception {
        Field cachedSecretField = CloudWatchLogsWebhookHandler.class.getDeclaredField("cachedWebhookSecret");
        cachedSecretField.setAccessible(true);
        cachedSecretField.set(null, null);
    }
    
    /**
     * Helper method to set the cached webhook secret using reflection
     */
    private void setCachedSecret(String secret) throws Exception {
        Field cachedSecretField = CloudWatchLogsWebhookHandler.class.getDeclaredField("cachedWebhookSecret");
        cachedSecretField.setAccessible(true);
        cachedSecretField.set(null, secret);
    }
    
    /**
     * Helper method to invoke private getWebhookSecret method using reflection
     */
    private String invokeGetWebhookSecret(Context context) throws Exception {
        Method method = CloudWatchLogsWebhookHandler.class.getDeclaredMethod("getWebhookSecret", Context.class);
        method.setAccessible(true);
        return (String) method.invoke(handler, context);
    }
    
    @Test
    void shouldReturnCachedSecretWhenAvailable() throws Exception {
        // Arrange
        String expectedSecret = "cached-secret-value";
        setCachedSecret(expectedSecret);
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(expectedSecret, result);
    }
    
    @Test
    void shouldReturnNullWhenSecretArnIsNull() throws Exception {
        // Arrange
        // SECRET_ARN environment variable is not set (null)
        resetCachedSecret();
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void shouldHandleMultipleCallsWithCache() throws Exception {
        // Arrange
        String expectedSecret = "my-cached-secret";
        setCachedSecret(expectedSecret);
        
        // Act - Call multiple times
        String result1 = invokeGetWebhookSecret(mockContext);
        String result2 = invokeGetWebhookSecret(mockContext);
        String result3 = invokeGetWebhookSecret(mockContext);
        
        // Assert - All should return the same cached value
        assertEquals(expectedSecret, result1);
        assertEquals(expectedSecret, result2);
        assertEquals(expectedSecret, result3);
    }
    
    @Test
    void shouldHandleCachedSecretWithSpecialCharacters() throws Exception {
        // Arrange
        String specialSecret = "secret!@#$%^&*()_+-={}[]|:;<>?,./";
        setCachedSecret(specialSecret);
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(specialSecret, result);
    }
    
    @Test
    void shouldHandleCachedSecretWithUnicode() throws Exception {
        // Arrange
        String unicodeSecret = "秘密🔐🌍";
        setCachedSecret(unicodeSecret);
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(unicodeSecret, result);
    }
    
    @Test
    void shouldHandleLongCachedSecret() throws Exception {
        // Arrange
        String longSecret = "a".repeat(10000);
        setCachedSecret(longSecret);
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(longSecret, result);
        assertEquals(10000, result.length());
    }
    
    @Test
    void shouldHandleEmptyStringCachedSecret() throws Exception {
        // Arrange
        setCachedSecret("");
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals("", result);
    }
    
    @Test
    void shouldPreferCacheOverFetching() throws Exception {
        // Arrange
        String cachedValue = "cached-value";
        setCachedSecret(cachedValue);
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(cachedValue, result);
    }
    
    @Test
    void shouldReturnConsistentCachedValue() throws Exception {
        // Arrange
        String secret = "consistent-secret-123";
        setCachedSecret(secret);
        
        // Act - Multiple calls in sequence
        String result1 = invokeGetWebhookSecret(mockContext);
        String result2 = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(result1, result2);
        assertNotNull(result1);
    }
    
    @Test
    void shouldHandleWhitespaceCachedSecret() throws Exception {
        // Arrange
        String whitespaceSecret = "   secret with spaces   ";
        setCachedSecret(whitespaceSecret);
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(whitespaceSecret, result);
    }
    
    @Test
    void shouldHandleNewlineCachedSecret() throws Exception {
        // Arrange
        String newlineSecret = "line1\nline2\nline3";
        setCachedSecret(newlineSecret);
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(newlineSecret, result);
        assertTrue(result.contains("\n"));
    }
    
    @Test
    void shouldHandleJsonFormattedCachedSecret() throws Exception {
        // Arrange
        String jsonSecret = "{\"key\":\"value\",\"nested\":{\"data\":\"secret\"}}";
        setCachedSecret(jsonSecret);
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(jsonSecret, result);
    }
    
    @Test
    void shouldHandleBase64CachedSecret() throws Exception {
        // Arrange
        String base64Secret = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=";
        setCachedSecret(base64Secret);
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(base64Secret, result);
    }
    
    @Test
    void shouldHandleNumericCachedSecret() throws Exception {
        // Arrange
        String numericSecret = "1234567890";
        setCachedSecret(numericSecret);
        
        // Act
        String result = invokeGetWebhookSecret(mockContext);
        
        // Assert
        assertEquals(numericSecret, result);
    }
    
    @Test
    void shouldNotModifyCachedSecretOnRetrieval() throws Exception {
        // Arrange
        String originalSecret = "original-secret";
        setCachedSecret(originalSecret);
        
        // Act
        String result1 = invokeGetWebhookSecret(mockContext);
        String result2 = invokeGetWebhookSecret(mockContext);
        
        // Modify result1 (if it were mutable)
        String modified = result1 + "-modified";
        
        // Assert - result2 should still be original
        assertEquals(originalSecret, result2);
        assertNotEquals(modified, result2);
    }
}
