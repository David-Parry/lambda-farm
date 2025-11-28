package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for MagicNumberHandler
 * Tests validation of the magicNumber query parameter
 */
public class MagicNumberHandlerTest {
    
    private MagicNumberHandler handler;
    private Context mockContext;
    private LambdaLogger mockLogger;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    public void setUp() {
        handler = new MagicNumberHandler();
        mockContext = Mockito.mock(Context.class);
        mockLogger = Mockito.mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
        objectMapper = new ObjectMapper();
    }
    
    @Test
    public void testValidMagicNumber_ReturnsSuccess() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "42");
        request.setQueryStringParameters(queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(42, responseBody.get("magicNumber").asInt());
        assertTrue(responseBody.get("isEven").asBoolean());
        assertEquals(1764, responseBody.get("squared").asInt());
        assertTrue(responseBody.get("message").asText().contains("Successfully processed"));
        
        // Verify CORS headers
        assertNotNull(response.getHeaders());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
    }
    
    @Test
    public void testValidNegativeMagicNumber_ReturnsSuccess() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "-5");
        request.setQueryStringParameters(queryParams);
        
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
    public void testMissingQueryParameters_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(null);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Missing required query parameter"));
        assertTrue(responseBody.get("error").asText().contains("magicNumber"));
    }
    
    @Test
    public void testMissingMagicNumberParameter_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("otherParam", "value");
        request.setQueryStringParameters(queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Missing required query parameter"));
    }
    
    @Test
    public void testEmptyMagicNumber_Returns400() throws Exception {
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
        assertTrue(responseBody.get("error").asText().contains("cannot be empty"));
    }
    
    @Test
    public void testWhitespaceMagicNumber_Returns400() throws Exception {
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
        assertTrue(responseBody.get("error").asText().contains("cannot be empty"));
    }
    
    @Test
    public void testNonNumericMagicNumber_Returns400() throws Exception {
        // Arrange - This is the exact scenario from the bug report
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "I3");
        request.setQueryStringParameters(queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("must be a valid integer"));
        
        // Verify this is NOT a 500 error
        assertNotEquals(500, response.getStatusCode());
    }
    
    @Test
    public void testAlphanumericMagicNumber_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "abc123");
        request.setQueryStringParameters(queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("must be a valid integer"));
    }
    
    @Test
    public void testDecimalMagicNumber_Returns400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "3.14");
        request.setQueryStringParameters(queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("must be a valid integer"));
    }
    
    @Test
    public void testMagicNumberWithLeadingWhitespace_ReturnsSuccess() throws Exception {
        // Arrange - Should trim and parse successfully
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "  10  ");
        request.setQueryStringParameters(queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(10, responseBody.get("magicNumber").asInt());
    }
    
    @Test
    public void testZeroMagicNumber_ReturnsSuccess() throws Exception {
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
}
