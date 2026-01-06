package com.davidparry.lambda;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CloudWatchLogsWebhookHandler
 * Tests core logic methods without external dependencies
 */
class CloudWatchLogsWebhookHandlerTest {
    
    private CloudWatchLogsWebhookHandler handler;
    
    @BeforeEach
    void setUp() {
        handler = new CloudWatchLogsWebhookHandler();
    }
    
    @Test
    void shouldGenerateValidHmacSignature() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "{\"test\":\"data\"}";
        String secret = "my-secret-key";
        
        // Act
        String signature = handler.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertTrue(signature.startsWith("sha256="));
        assertEquals(71, signature.length()); // "sha256=" + 64 hex chars
        
        // Verify signature is deterministic
        String signature2 = handler.generateSignature(payload, secret);
        assertEquals(signature, signature2);
    }
    
    @Test
    void shouldGenerateDifferentSignaturesForDifferentPayloads() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload1 = "{\"test\":\"data1\"}";
        String payload2 = "{\"test\":\"data2\"}";
        String secret = "my-secret-key";
        
        // Act
        String signature1 = handler.generateSignature(payload1, secret);
        String signature2 = handler.generateSignature(payload2, secret);
        
        // Assert
        assertNotEquals(signature1, signature2);
    }
    
    @Test
    void shouldGenerateDifferentSignaturesForDifferentSecrets() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "{\"test\":\"data\"}";
        String secret1 = "secret1";
        String secret2 = "secret2";
        
        // Act
        String signature1 = handler.generateSignature(payload, secret1);
        String signature2 = handler.generateSignature(payload, secret2);
        
        // Assert
        assertNotEquals(signature1, signature2);
    }
    
    @Test
    void shouldGenerateSignatureWithExpectedFormat() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "test payload";
        String secret = "test-secret";
        
        // Act
        String signature = handler.generateSignature(payload, secret);
        
        // Assert
        assertTrue(signature.matches("^sha256=[a-f0-9]{64}$"));
    }
    
    @Test
    void shouldGenerateCorrectHmacSha256() throws Exception {
        // Arrange
        String payload = "Hello, World!";
        String secret = "secret-key";
        
        // Calculate expected signature manually
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder expectedHex = new StringBuilder("sha256=");
        for (byte b : hash) {
            expectedHex.append(String.format("%02x", b));
        }
        
        // Act
        String signature = handler.generateSignature(payload, secret);
        
        // Assert
        assertEquals(expectedHex.toString(), signature);
    }
    
    @Test
    void shouldHandleEmptyPayloadSignature() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "";
        String secret = "secret";
        
        // Act
        String signature = handler.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertTrue(signature.startsWith("sha256="));
    }
    
    @Test
    void shouldHandleSpecialCharactersInPayload() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "{\"message\":\"Error: Something went wrong! @#$%^&*()\"}";
        String secret = "secret";
        
        // Act
        String signature = handler.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertTrue(signature.startsWith("sha256="));
        assertEquals(71, signature.length());
    }
    
    @Test
    void shouldHandleUnicodeInPayload() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "{\"message\":\"Hello 世界 🌍\"}";
        String secret = "secret";
        
        // Act
        String signature = handler.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertTrue(signature.startsWith("sha256="));
    }
    
    @Test
    void shouldHandleLargePayload() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        StringBuilder largePayload = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largePayload.append("data");
        }
        String secret = "secret";
        
        // Act
        String signature = handler.generateSignature(largePayload.toString(), secret);
        
        // Assert
        assertNotNull(signature);
        assertTrue(signature.startsWith("sha256="));
        assertEquals(71, signature.length());
    }
    
    @Test
    void shouldProduceConsistentSignaturesAcrossInvocations() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "{\"consistent\":\"test\"}";
        String secret = "consistent-secret";
        
        // Act - Generate signature multiple times
        String sig1 = handler.generateSignature(payload, secret);
        String sig2 = handler.generateSignature(payload, secret);
        String sig3 = handler.generateSignature(payload, secret);
        
        // Assert - All signatures should be identical
        assertEquals(sig1, sig2);
        assertEquals(sig2, sig3);
    }
    
    @Test
    void shouldHandleJsonPayloadWithNestedObjects() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "{\"outer\":{\"inner\":{\"deep\":\"value\"}}}";
        String secret = "secret";
        
        // Act
        String signature = handler.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertTrue(signature.startsWith("sha256="));
    }
    
    @Test
    void shouldHandleJsonArrayPayload() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "[{\"id\":1},{\"id\":2},{\"id\":3}]";
        String secret = "secret";
        
        // Act
        String signature = handler.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertTrue(signature.startsWith("sha256="));
    }
    
    @Test
    void shouldHandleWhitespaceInPayload() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload1 = "{\"test\":\"data\"}";
        String payload2 = "{ \"test\" : \"data\" }";
        String secret = "secret";
        
        // Act
        String sig1 = handler.generateSignature(payload1, secret);
        String sig2 = handler.generateSignature(payload2, secret);
        
        // Assert - Different whitespace should produce different signatures
        assertNotEquals(sig1, sig2);
    }
    
    @Test
    void shouldHandleLongSecret() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "{\"test\":\"data\"}";
        String longSecret = "a".repeat(1000);
        
        // Act
        String signature = handler.generateSignature(payload, longSecret);
        
        // Assert
        assertNotNull(signature);
        assertTrue(signature.startsWith("sha256="));
    }
    
    @Test
    void shouldHandleNumericPayload() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "123456789";
        String secret = "secret";
        
        // Act
        String signature = handler.generateSignature(payload, secret);
        
        // Assert
        assertNotNull(signature);
        assertTrue(signature.startsWith("sha256="));
    }
    
    @Test
    void shouldVerifySignatureHexFormat() throws NoSuchAlgorithmException, InvalidKeyException {
        // Arrange
        String payload = "test";
        String secret = "secret";
        
        // Act
        String signature = handler.generateSignature(payload, secret);
        String hexPart = signature.substring(7); // Remove "sha256=" prefix
        
        // Assert - Verify all characters are valid hex
        assertTrue(hexPart.matches("[a-f0-9]+"));
        assertEquals(64, hexPart.length());
    }
}
