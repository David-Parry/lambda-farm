package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for MagicNumberHandler
 * Tests all public methods, edge cases, and error handling
 */
@ExtendWith(MockitoExtension.class)
class MagicNumberHandlerTest {
    
    private MagicNumberHandler handler;
    private ObjectMapper objectMapper;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private LambdaLogger mockLogger;
    
    @BeforeEach
    void setUp() {
        handler = new MagicNumberHandler();
        objectMapper = new ObjectMapper();
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }
    
    /**
     * Helper method to create GET request with magic number parameter
     */
    private APIGatewayProxyRequestEvent createGetRequest(String magicNumber) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        
        Map<String, String> queryParams = new HashMap<>();
        if (magicNumber != null) {
            queryParams.put("magicNumber", magicNumber);
        }
        request.setQueryStringParameters(queryParams);
        
        return request;
    }
    
    /**
     * Helper method to create OPTIONS request for CORS preflight
     */
    private APIGatewayProxyRequestEvent createOptionsRequest() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("OPTIONS");
        return request;
    }
    
    @Test
    void shouldProcessValidPositiveMagicNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("42");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(42, responseBody.get("magicNumber").asInt());
        assertEquals(true, responseBody.get("isEven").asBoolean());
        assertEquals(1764, responseBody.get("squared").asInt());
        assertTrue(responseBody.get("message").asText().contains("42"));
        
        // Verify CORS headers
        assertNotNull(response.getHeaders());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
    }
    
    @Test
    void shouldProcessValidNegativeMagicNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("-15");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(-15, responseBody.get("magicNumber").asInt());
        assertEquals(false, responseBody.get("isEven").asBoolean());
        assertEquals(225, responseBody.get("squared").asInt());
    }
    
    @Test
    void shouldProcessZeroMagicNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("0");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(0, responseBody.get("magicNumber").asInt());
        assertEquals(true, responseBody.get("isEven").asBoolean());
        assertEquals(0, responseBody.get("squared").asInt());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"1", "100", "999", "-1", "-100"})
    void shouldProcessVariousMagicNumbers(String magicNumber) throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest(magicNumber);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(Integer.parseInt(magicNumber), responseBody.get("magicNumber").asInt());
    }
    
    @Test
    void shouldHandleMaxIntegerValue() throws Exception {
        // Arrange
        String maxInt = String.valueOf(Integer.MAX_VALUE);
        APIGatewayProxyRequestEvent request = createGetRequest(maxInt);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(Integer.MAX_VALUE, responseBody.get("magicNumber").asInt());
    }
    
    @Test
    void shouldHandleMinIntegerValue() throws Exception {
        // Arrange
        String minInt = String.valueOf(Integer.MIN_VALUE);
        APIGatewayProxyRequestEvent request = createGetRequest(minInt);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(Integer.MIN_VALUE, responseBody.get("magicNumber").asInt());
    }
    
    @Test
    void shouldReturnErrorForInvalidNumberFormat() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("not-a-number");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(500, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.has("error"));
        assertEquals(500, responseBody.get("statusCode").asInt());
    }
    
    @Test
    void shouldReturnErrorForMissingMagicNumberParameter() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest(null);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(500, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.has("error"));
    }
    
    @Test
    void shouldReturnErrorForNullQueryParameters() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(null);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(500, response.getStatusCode());
        assertNotNull(response.getBody());
    }
    
    @Test
    void shouldHandleOptionsPreflightRequest() {
        // Arrange
        APIGatewayProxyRequestEvent request = createOptionsRequest();
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertEquals("{}", response.getBody());
        
        // Verify CORS headers are present
        assertNotNull(response.getHeaders());
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
        assertEquals("GET, OPTIONS", response.getHeaders().get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type, x-api-key", response.getHeaders().get("Access-Control-Allow-Headers"));
    }
    
    @Test
    void shouldIncludeCorsHeadersInAllResponses() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("123");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET, OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type, x-api-key", headers.get("Access-Control-Allow-Headers"));
    }
    
    @Test
    void shouldIncludeCorsHeadersInErrorResponses() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("invalid");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(500, response.getStatusCode());
        
        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
    }
    
    @Test
    void shouldLogReceivedRequest() {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("42");
        
        // Act
        handler.handleRequest(request, mockContext);
        
        // Assert
        verify(mockLogger, atLeastOnce()).log(anyString());
    }
    
    @Test
    void shouldLogSuccessfulProcessing() {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("42");
        
        // Act
        handler.handleRequest(request, mockContext);
        
        // Assert
        verify(mockLogger).log(contains("Successfully processed"));
    }
    
    @Test
    void shouldLogErrorsWithStackTrace() {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("invalid");
        
        // Act
        handler.handleRequest(request, mockContext);
        
        // Assert
        verify(mockLogger).log(contains("ERROR"));
    }
    
    @Test
    void shouldReturnValidJsonStructureOnSuccess() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("7");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.has("magicNumber"));
        assertTrue(responseBody.has("isEven"));
        assertTrue(responseBody.has("squared"));
        assertTrue(responseBody.has("message"));
    }
    
    @Test
    void shouldCalculateIsEvenCorrectly() throws Exception {
        // Test even number
        APIGatewayProxyRequestEvent evenRequest = createGetRequest("10");
        APIGatewayProxyResponseEvent evenResponse = handler.handleRequest(evenRequest, mockContext);
        JsonNode evenBody = objectMapper.readTree(evenResponse.getBody());
        assertTrue(evenBody.get("isEven").asBoolean());
        
        // Test odd number
        APIGatewayProxyRequestEvent oddRequest = createGetRequest("11");
        APIGatewayProxyResponseEvent oddResponse = handler.handleRequest(oddRequest, mockContext);
        JsonNode oddBody = objectMapper.readTree(oddResponse.getBody());
        assertFalse(oddBody.get("isEven").asBoolean());
    }
    
    @Test
    void shouldCalculateSquaredCorrectly() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createGetRequest("5");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(25, responseBody.get("squared").asInt());
    }
}
