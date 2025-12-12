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
        doNothing().when(mockLogger).log(anyString());
    }

    @Test
    void testValidMagicNumber_Success() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "42");
        request.setQueryStringParameters(queryParams);
        request.setHttpMethod("GET");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(42, responseBody.get("magicNumber").asInt());
        assertTrue(responseBody.get("isEven").asBoolean());
        assertEquals(1764, responseBody.get("squared").asInt());
        assertTrue(responseBody.get("message").asText().contains("42"));
    }

    @Test
    void testValidMagicNumber_OddNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "7");
        request.setQueryStringParameters(queryParams);
        request.setHttpMethod("GET");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(7, responseBody.get("magicNumber").asInt());
        assertFalse(responseBody.get("isEven").asBoolean());
        assertEquals(49, responseBody.get("squared").asInt());
    }

    @Test
    void testMissingQueryParameters_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(null); // No query parameters
        request.setHttpMethod("GET");

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
    void testMissingMagicNumberParameter_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("otherParam", "value");
        request.setQueryStringParameters(queryParams);
        request.setHttpMethod("GET");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("magicNumber"));
        assertTrue(responseBody.get("error").asText().contains("required"));
    }

    @Test
    void testEmptyMagicNumberParameter_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "");
        request.setQueryStringParameters(queryParams);
        request.setHttpMethod("GET");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("required"));
    }

    @Test
    void testInvalidMagicNumber_NonNumeric_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "123abcert"); // Invalid format - matches the error from logs
        request.setQueryStringParameters(queryParams);
        request.setHttpMethod("GET");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("valid integer"));
        assertEquals(400, responseBody.get("statusCode").asInt());
    }

    @Test
    void testInvalidMagicNumber_AlphaOnly_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "abc");
        request.setQueryStringParameters(queryParams);
        request.setHttpMethod("GET");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("valid integer"));
    }

    @Test
    void testOptionsRequest_Returns200() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("OPTIONS");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertEquals("{}", response.getBody());
        assertNotNull(response.getHeaders());
        assertTrue(response.getHeaders().containsKey("Access-Control-Allow-Origin"));
    }

    @Test
    void testNegativeNumber_Success() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "-5");
        request.setQueryStringParameters(queryParams);
        request.setHttpMethod("GET");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(-5, responseBody.get("magicNumber").asInt());
        assertFalse(responseBody.get("isEven").asBoolean());
        assertEquals(25, responseBody.get("squared").asInt());
    }

    @Test
    void testZero_Success() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "0");
        request.setQueryStringParameters(queryParams);
        request.setHttpMethod("GET");

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
    void testWhitespaceAroundNumber_Success() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "  100  ");
        request.setQueryStringParameters(queryParams);
        request.setHttpMethod("GET");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(100, responseBody.get("magicNumber").asInt());
    }

    @Test
    void testCorsHeaders_Present() {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "1");
        request.setQueryStringParameters(queryParams);
        request.setHttpMethod("GET");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET, OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertTrue(headers.get("Access-Control-Allow-Headers").contains("Content-Type"));
    }
}
