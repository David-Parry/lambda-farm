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
    public void testValidMagicNumber() throws Exception {
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
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(42, responseBody.get("magicNumber").asInt());
        assertEquals(true, responseBody.get("isEven").asBoolean());
        assertEquals(1764, responseBody.get("squared").asInt());
        assertTrue(responseBody.get("message").asText().contains("42"));
    }
    
    @Test
    public void testMissingQueryParameters() throws Exception {
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
        assertEquals("Missing query parameters", responseBody.get("error").asText());
        assertEquals(400, responseBody.get("statusCode").asInt());
    }
    
    @Test
    public void testEmptyQueryParameters() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(new HashMap<>());
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("Missing query parameters", responseBody.get("error").asText());
    }
    
    @Test
    public void testMissingMagicNumberParameter() throws Exception {
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
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("magicNumber parameter is required", responseBody.get("error").asText());
        assertEquals(400, responseBody.get("statusCode").asInt());
    }
    
    @Test
    public void testNonNumericMagicNumber() throws Exception {
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
        assertEquals("magicNumber must be a valid integer", responseBody.get("error").asText());
        assertEquals(400, responseBody.get("statusCode").asInt());
    }
    
    @Test
    public void testPartiallyNumericMagicNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "abc123");
        request.setQueryStringParameters(queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("magicNumber must be a valid integer", responseBody.get("error").asText());
    }
    
    @Test
    public void testEmptyMagicNumberParameter() throws Exception {
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
        assertEquals("magicNumber parameter is required", responseBody.get("error").asText());
    }
    
    @Test
    public void testWhitespaceMagicNumberParameter() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "   ");
        request.setQueryStringParameters(queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals("magicNumber parameter is required", responseBody.get("error").asText());
    }
    
    @Test
    public void testNegativeMagicNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "-15");
        request.setQueryStringParameters(queryParams);
        
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
    public void testZeroMagicNumber() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "0");
        request.setQueryStringParameters(queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(0, responseBody.get("magicNumber").asInt());
        assertEquals(true, responseBody.get("isEven").asBoolean());
        assertEquals(0, responseBody.get("squared").asInt());
    }
    
    @Test
    public void testOptionsRequest() throws Exception {
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
    public void testMagicNumberWithLeadingTrailingSpaces() throws Exception {
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
        assertEquals(true, responseBody.get("isEven").asBoolean());
        assertEquals(10000, responseBody.get("squared").asInt());
    }
}
