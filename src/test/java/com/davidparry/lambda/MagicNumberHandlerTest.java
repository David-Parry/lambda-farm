package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MagicNumberHandler
 */
class MagicNumberHandlerTest {

    private MagicNumberHandler handler;
    private ObjectMapper objectMapper;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new MagicNumberHandler();
        objectMapper = new ObjectMapper();
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Test
    void testValidIntegerInput() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "10");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(10, responseBody.get("magicNumber").asInt());
        assertTrue(responseBody.get("isEven").asBoolean());
        assertEquals(100, responseBody.get("squared").asInt());
        
        // Verify headers
        assertTrue(response.getHeaders().containsKey("Content-Type"));
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    void testMissingQueryParameters() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(null);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("magicNumber"));
        assertTrue(responseBody.get("error").asText().contains("required"));
        assertEquals(400, responseBody.get("statusCode").asInt());
    }

    @Test
    void testEmptyMagicNumberValue() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("magicNumber"));
        assertTrue(responseBody.get("error").asText().contains("required"));
    }

    @Test
    void testNonNumericInput() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "I78");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("valid integer"));
        assertTrue(responseBody.get("error").asText().contains("I78"));
        assertEquals(400, responseBody.get("statusCode").asInt());
    }

    @Test
    void testNegativeNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "-5");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(-5, responseBody.get("magicNumber").asInt());
        assertFalse(responseBody.get("isEven").asBoolean());
        assertEquals(25, responseBody.get("squared").asInt());
    }

    @Test
    void testLargeInteger() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "2147483647"); // Integer.MAX_VALUE
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(2147483647, responseBody.get("magicNumber").asInt());
    }

    @Test
    void testWhitespaceHandling() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "  42  ");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(42, responseBody.get("magicNumber").asInt());
    }

    @Test
    void testOddNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "7");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(7, responseBody.get("magicNumber").asInt());
        assertFalse(responseBody.get("isEven").asBoolean());
        assertEquals(49, responseBody.get("squared").asInt());
    }

    @Test
    void testZero() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "0");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(0, responseBody.get("magicNumber").asInt());
        assertTrue(responseBody.get("isEven").asBoolean());
        assertEquals(0, responseBody.get("squared").asInt());
    }

    @Test
    void testCorsHeaders() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "5");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET, OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type", headers.get("Access-Control-Allow-Headers"));
    }
}
