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
 * Unit tests for MagicNumberHandler to validate input validation and error handling
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
    void testValidMagicNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "42");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(42, responseBody.get("magicNumber").asInt());
        assertEquals(true, responseBody.get("isEven").asBoolean());
        assertEquals(1764, responseBody.get("squared").asInt());
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
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("Required query parameter 'magicNumber' is missing", responseBody.get("error").asText());
        assertEquals(400, responseBody.get("statusCode").asInt());
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
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("Required query parameter 'magicNumber' is missing", responseBody.get("error").asText());
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
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("Required query parameter 'magicNumber' is missing", responseBody.get("error").asText());
    }

    @Test
    void testInvalidMagicNumberFormat() throws Exception {
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
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("Query parameter 'magicNumber' must be an integer", responseBody.get("error").asText());
    }

    @Test
    void testNonNumericMagicNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "abc");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("Query parameter 'magicNumber' must be an integer", responseBody.get("error").asText());
    }

    @Test
    void testOptionsRequest() {
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
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(-5, responseBody.get("magicNumber").asInt());
        assertEquals(false, responseBody.get("isEven").asBoolean());
        assertEquals(25, responseBody.get("squared").asInt());
    }

    @Test
    void testMagicNumberWithWhitespace() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "  100  ");
        request.setQueryStringParameters(queryParams);

        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        // Assert
        assertEquals(200, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(100, responseBody.get("magicNumber").asInt());
    }
}
