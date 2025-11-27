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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MagicNumberHandler
 * Tests cover validation scenarios for null parameters, missing parameters, 
 * invalid formats, and valid inputs
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
        doNothing().when(mockLogger).log(anyString());
    }

    @Test
    void testValidInput_PositiveNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "5");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(5, responseBody.get("magicNumber").asInt());
        assertFalse(responseBody.get("isEven").asBoolean());
        assertEquals(25, responseBody.get("squared").asInt());
        assertTrue(responseBody.get("message").asText().contains("Successfully processed"));
    }

    @Test
    void testValidInput_NegativeNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "-10");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(-10, responseBody.get("magicNumber").asInt());
        assertTrue(responseBody.get("isEven").asBoolean());
        assertEquals(100, responseBody.get("squared").asInt());
    }

    @Test
    void testValidInput_Zero() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "0");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(0, responseBody.get("magicNumber").asInt());
        assertTrue(responseBody.get("isEven").asBoolean());
        assertEquals(0, responseBody.get("squared").asInt());
    }

    @Test
    void testMissingQueryStringParameters() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(null);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Missing required query parameter 'magicNumber'"));
    }

    @Test
    void testMissingMagicNumberParameter() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        // magicNumber key is not present
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Missing required query parameter 'magicNumber'"));
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
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Missing required query parameter 'magicNumber'"));
    }

    @Test
    void testWhitespaceMagicNumberValue() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "   ");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Missing required query parameter 'magicNumber'"));
    }

    @Test
    void testNonNumericMagicNumber_AlphanumericString() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "123abcert");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("must be a valid integer"));
    }

    @Test
    void testNonNumericMagicNumber_AlphabeticString() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "abc");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("must be a valid integer"));
    }

    @Test
    void testNonNumericMagicNumber_DecimalNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "12.34");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("must be a valid integer"));
    }

    @Test
    void testNonNumericMagicNumber_SpecialCharacters() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "!@#$");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("must be a valid integer"));
    }

    @Test
    void testResponseHeaders() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "42");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET, OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type", headers.get("Access-Control-Allow-Headers"));
    }

    @Test
    void testLargePositiveNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "999999");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(999999, responseBody.get("magicNumber").asInt());
    }

    @Test
    void testLargeNegativeNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "-999999");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(-999999, responseBody.get("magicNumber").asInt());
    }
}
