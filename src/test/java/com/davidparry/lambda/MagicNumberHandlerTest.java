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
import static org.mockito.Mockito.doAnswer;

/**
 * Comprehensive test suite for MagicNumberHandler
 * Tests all validation scenarios including the exact error from CloudWatch logs
 */
public class MagicNumberHandlerTest {
    
    private MagicNumberHandler handler;
    private ObjectMapper objectMapper;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private LambdaLogger mockLogger;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new MagicNumberHandler();
        objectMapper = new ObjectMapper();
        
        // Setup mock logger - use doAnswer for void method
        doAnswer(invocation -> null).when(mockLogger).log(anyString());
        org.mockito.Mockito.when(mockContext.getLogger()).thenReturn(mockLogger);
    }
    
    @Test
    public void testValidMagicNumber_Positive() throws Exception {
        APIGatewayProxyRequestEvent request = createRequest("42");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(42, body.get("magicNumber").asInt());
        assertTrue(body.get("isEven").asBoolean());
        assertEquals(1764, body.get("squared").asInt());
        assertNotNull(body.get("message"));
        assertCorsHeaders(response);
    }
    
    @Test
    public void testValidMagicNumber_Negative() throws Exception {
        APIGatewayProxyRequestEvent request = createRequest("-15");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(-15, body.get("magicNumber").asInt());
        assertFalse(body.get("isEven").asBoolean());
        assertEquals(225, body.get("squared").asInt());
    }
    
    @Test
    public void testValidMagicNumber_Zero() throws Exception {
        APIGatewayProxyRequestEvent request = createRequest("0");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(0, body.get("magicNumber").asInt());
        assertTrue(body.get("isEven").asBoolean());
    }
    
    @Test
    public void testValidMagicNumber_WithWhitespace() throws Exception {
        APIGatewayProxyRequestEvent request = createRequest("  123  ");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(123, body.get("magicNumber").asInt());
    }
    
    @Test
    public void testNullQueryParameters() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(null);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("Query string parameters are required"));
        assertCorsHeaders(response);
    }
    
    @Test
    public void testEmptyQueryParameters() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setQueryStringParameters(new HashMap<>());
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("magicNumber"));
        assertTrue(body.get("error").asText().contains("missing"));
    }
    
    @Test
    public void testMissingMagicNumberParameter() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> params = new HashMap<>();
        params.put("otherParam", "value");
        request.setQueryStringParameters(params);
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("magicNumber"));
    }
    
    @Test
    public void testEmptyMagicNumberParameter() throws Exception {
        APIGatewayProxyRequestEvent request = createRequest("");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("missing or empty"));
    }
    
    @Test
    public void testWhitespaceOnlyMagicNumber() throws Exception {
        APIGatewayProxyRequestEvent request = createRequest("   ");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("missing or empty"));
    }
    
    @Test
    public void testInvalidFormat_Alphanumeric_ExactCloudWatchError() throws Exception {
        // This is the exact error from CloudWatch logs: "123abcert"
        APIGatewayProxyRequestEvent request = createRequest("123abcert");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("Invalid magicNumber format"));
        assertTrue(body.get("error").asText().contains("123abcert"));
        assertCorsHeaders(response);
    }
    
    @Test
    public void testInvalidFormat_LettersOnly() throws Exception {
        APIGatewayProxyRequestEvent request = createRequest("abc");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("Invalid magicNumber format"));
    }
    
    @Test
    public void testInvalidFormat_Decimal() throws Exception {
        APIGatewayProxyRequestEvent request = createRequest("123.45");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("Invalid magicNumber format"));
    }
    
    @Test
    public void testInvalidFormat_TooLarge() throws Exception {
        APIGatewayProxyRequestEvent request = createRequest("999999999999999999999");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(400, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(400, body.get("statusCode").asInt());
        assertTrue(body.get("error").asText().contains("Invalid magicNumber format"));
    }
    
    @Test
    public void testOptionsRequest_CorsPreFlight() throws Exception {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("OPTIONS");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertEquals(200, response.getStatusCode());
        assertEquals("{}", response.getBody());
        assertCorsHeaders(response);
    }
    
    @Test
    public void testCorsHeaders() throws Exception {
        APIGatewayProxyRequestEvent request = createRequest("42");
        
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        assertCorsHeaders(response);
    }
    
    // Helper methods
    
    private APIGatewayProxyRequestEvent createRequest(String magicNumber) {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        Map<String, String> params = new HashMap<>();
        params.put("magicNumber", magicNumber);
        request.setQueryStringParameters(params);
        return request;
    }
    
    private void assertCorsHeaders(APIGatewayProxyResponseEvent response) {
        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("GET, OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertEquals("Content-Type, x-api-key", headers.get("Access-Control-Allow-Headers"));
    }
}
