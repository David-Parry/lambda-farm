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
    void testValidPositiveNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("4");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(4, body.get("magicNumber").asInt());
        assertTrue(body.get("isEven").asBoolean());
        assertEquals(16, body.get("squared").asInt());
        assertTrue(body.get("message").asText().contains("Successfully processed"));
    }

    @Test
    void testValidNegativeNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("-3");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(-3, body.get("magicNumber").asInt());
        assertFalse(body.get("isEven").asBoolean());
        assertEquals(9, body.get("squared").asInt());
    }

    @Test
    void testInvalidNumberWithLetters() throws Exception {
        // Arrange - this is the bug case from the issue
        APIGatewayProxyRequestEvent request = createRequest("23l");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("Invalid magicNumber"));
        assertTrue(body.get("error").asText().contains("whole number"));
    }

    @Test
    void testInvalidNumberWithDecimal() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("1.5");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("Invalid magicNumber"));
    }

    @Test
    void testInvalidNumberWithText() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("abc");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("Invalid magicNumber"));
    }

    @Test
    void testMissingQueryParameters() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(null);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("Missing query parameters"));
    }

    @Test
    void testMissingMagicNumberParameter() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("otherParam", "value");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("Missing required query parameter"));
    }

    @Test
    void testEmptyMagicNumberParameter() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("cannot be empty"));
    }

    @Test
    void testWhitespaceMagicNumberParameter() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("   ");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("cannot be empty"));
    }

    @Test
    void testValidNumberWithWhitespace() throws Exception {
        // Arrange - should handle trimming
        APIGatewayProxyRequestEvent request = createRequest("  42  ");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(42, body.get("magicNumber").asInt());
    }

    @Test
    void testOptionsRequest() throws Exception {
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
        APIGatewayProxyRequestEvent request = createRequest("5");

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
    void testZeroValue() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("0");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(0, body.get("magicNumber").asInt());
        assertTrue(body.get("isEven").asBoolean());
        assertEquals(0, body.get("squared").asInt());
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
