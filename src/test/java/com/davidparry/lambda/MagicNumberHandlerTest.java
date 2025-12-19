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
        
        // Setup mock logger
        when(mockContext.getLogger()).thenReturn(mockLogger);
        doNothing().when(mockLogger).log(anyString());
    }
    
    @Test
    void testSuccessfulRequest_ValidPositiveNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("42");
        
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
    void testSuccessfulRequest_ValidNegativeNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("-15");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(-15, responseBody.get("magicNumber").asInt());
        assertFalse(responseBody.get("isEven").asBoolean());
        assertEquals(225, responseBody.get("squared").asInt());
    }
    
    @Test
    void testSuccessfulRequest_Zero() throws Exception {
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
    void testInvalidRequest_NonNumericValue() throws Exception {
        // Arrange - This is the bug scenario from the CloudWatch alert
        APIGatewayProxyRequestEvent request = createRequest("123abcert");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode()); // Should be 400, not 500
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Invalid format"));
        assertTrue(responseBody.get("error").asText().contains("123abcert"));
    }
    
    @Test
    void testInvalidRequest_AlphabeticValue() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("abc");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Invalid format"));
    }
    
    @Test
    void testInvalidRequest_MissingParameter() throws Exception {
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
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Missing required parameter"));
    }
    
    @Test
    void testInvalidRequest_NullQueryParameters() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(null); // Null query parameters
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Missing query parameters"));
    }
    
    @Test
    void testInvalidRequest_EmptyParameter() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Missing required parameter"));
    }
    
    @Test
    void testInvalidRequest_WhitespaceOnly() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("   ");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Missing required parameter"));
    }
    
    @Test
    void testInvalidRequest_DecimalNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("42.5");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Invalid format"));
    }
    
    @Test
    void testInvalidRequest_SpecialCharacters() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = createRequest("42!");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(400, responseBody.get("statusCode").asInt());
        assertTrue(responseBody.get("error").asText().contains("Invalid format"));
    }
    
    @Test
    void testOptionsRequest_ReturnsSuccess() {
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
    void testResponse_ContainsCorsHeaders() {
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
        assertEquals("Content-Type, x-api-key", headers.get("Access-Control-Allow-Headers"));
    }
    
    /**
     * Helper method to create a request with a magic number parameter
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
