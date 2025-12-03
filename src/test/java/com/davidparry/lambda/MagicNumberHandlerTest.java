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
 * Tests input validation, error handling, and successful processing
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
    void testValidMagicNumber_ReturnsSuccess() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("42");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(42, responseBody.get("magicNumber").asInt());
        assertTrue(responseBody.get("isEven").asBoolean());
        assertEquals(1764, responseBody.get("squared").asInt());
        assertTrue(responseBody.get("message").asText().contains("42"));
    }

    @Test
    void testNegativeMagicNumber_ReturnsSuccess() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("-10");

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
    void testZeroMagicNumber_ReturnsSuccess() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("0");

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
    void testInvalidMagicNumber_ScientificNotation_Returns400() throws Exception {
        // Arrange - This is the original bug case
        APIGatewayProxyRequestEvent request = createRequest("23e");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("Invalid 'magicNumber' parameter"));
        assertTrue(responseBody.get("error").asText().contains("23e"));
        assertEquals(400, responseBody.get("statusCode").asInt());
    }

    @Test
    void testInvalidMagicNumber_AlphabeticCharacters_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("abc");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("Invalid 'magicNumber' parameter"));
        assertTrue(responseBody.get("error").asText().contains("abc"));
    }

    @Test
    void testInvalidMagicNumber_FloatingPoint_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("3.14");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("Invalid 'magicNumber' parameter"));
    }

    @Test
    void testMissingMagicNumber_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        // Don't add magicNumber parameter
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        // When query params exist but magicNumber is missing, we get this message
        assertTrue(responseBody.get("error").asText().contains("'magicNumber' parameter"));
    }

    @Test
    void testEmptyMagicNumber_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("Missing or empty 'magicNumber' parameter"));
    }

    @Test
    void testWhitespaceMagicNumber_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("   ");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("Missing or empty 'magicNumber' parameter"));
    }

    @Test
    void testNullQueryParameters_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(null);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("Missing query parameters"));
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
    void testResponseHasCorsHeaders() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("42");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET, OPTIONS", headers.get("Access-Control-Allow-Methods"));
    }

    @Test
    void testLargeValidInteger_ReturnsSuccess() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("999999");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(999999, responseBody.get("magicNumber").asInt());
    }

    @Test
    void testMixedAlphanumeric_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("123abc");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("Invalid 'magicNumber' parameter"));
    }

    /**
     * Helper method to create a request with a magicNumber query parameter
     */
    private APIGatewayProxyRequestEvent createRequest(String magicNumber) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", magicNumber);
        request.setQueryStringParameters(queryParams);
        
        return request;
    }
}
