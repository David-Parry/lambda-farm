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
        request.setHttpMethod("GET");
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
        assertTrue(responseBody.has("message"));
    }

    @Test
    void testInvalidNonNumericInput() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "123abcert");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("INVALID_MAGIC_NUMBER", responseBody.get("errorCode").asText());
        assertTrue(responseBody.get("message").asText().contains("valid integer"));
    }

    @Test
    void testMissingMagicNumberParameter() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        // magicNumber parameter is missing
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("MISSING_MAGIC_NUMBER", responseBody.get("errorCode").asText());
    }

    @Test
    void testNullQueryParameters() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(null);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("MISSING_PARAMETERS", responseBody.get("errorCode").asText());
    }

    @Test
    void testEmptyMagicNumberParameter() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("MISSING_MAGIC_NUMBER", responseBody.get("errorCode").asText());
    }

    @Test
    void testNegativeNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
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
    void testOptionsRequest() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("OPTIONS");

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        assertEquals("{}", response.getBody());
        assertTrue(response.getHeaders().containsKey("Access-Control-Allow-Origin"));
    }
}
