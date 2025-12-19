package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MagicNumberHandlerTest {

    private MagicNumberHandler handler;
    private Context mockContext;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new MagicNumberHandler();
        objectMapper = new ObjectMapper();
        
        // Create mock Context
        mockContext = new Context() {
            @Override
            public String getAwsRequestId() {
                return "test-request-id";
            }

            @Override
            public String getLogGroupName() {
                return "/aws/lambda/test-function";
            }

            @Override
            public String getLogStreamName() {
                return "2024/01/01/[$LATEST]test-stream";
            }

            @Override
            public String getFunctionName() {
                return "test-function";
            }

            @Override
            public String getFunctionVersion() {
                return "$LATEST";
            }

            @Override
            public String getInvokedFunctionArn() {
                return "arn:aws:lambda:us-east-1:123456789012:function:test-function";
            }

            @Override
            public com.amazonaws.services.lambda.runtime.CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public com.amazonaws.services.lambda.runtime.ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                return 30000;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 512;
            }

            @Override
            public LambdaLogger getLogger() {
                return new LambdaLogger() {
                    @Override
                    public void log(String message) {
                        System.out.println("LAMBDA LOG: " + message);
                    }

                    @Override
                    public void log(byte[] message) {
                        System.out.println("LAMBDA LOG: " + new String(message, StandardCharsets.UTF_8));
                    }
                };
            }
        };
    }

    @Test
    void testValidMagicNumber() throws Exception {
        // Create request with valid magic number
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "42");
        request.setQueryStringParameters(queryParams);
        
        // Execute handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Verify response
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // Parse and verify response body
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(42, responseBody.get("magicNumber").asInt());
        assertEquals(true, responseBody.get("isEven").asBoolean());
        assertEquals(1764, responseBody.get("squared").asInt());
        assertTrue(responseBody.get("message").asText().contains("Successfully processed"));
    }

    @Test
    void testMissingQueryParameters() throws Exception {
        // Create request with null query parameters
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(null);
        
        // Execute handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Verify error response
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("Missing required query parameter"));
    }

    @Test
    void testMissingMagicNumberParameter() throws Exception {
        // Create request without magicNumber parameter
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("otherParam", "value");
        request.setQueryStringParameters(queryParams);
        
        // Execute handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Verify error response
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("Missing required query parameter"));
    }

    @Test
    void testInvalidMagicNumber_NonNumeric() throws Exception {
        // Create request with non-numeric magic number (the original bug scenario)
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "123abcert");
        request.setQueryStringParameters(queryParams);
        
        // Execute handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Verify error response (should be 400, not 500)
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("must be a valid integer"));
    }

    @Test
    void testInvalidMagicNumber_Empty() throws Exception {
        // Create request with empty magic number
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "");
        request.setQueryStringParameters(queryParams);
        
        // Execute handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Verify error response
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("cannot be empty"));
    }

    @Test
    void testInvalidMagicNumber_Whitespace() throws Exception {
        // Create request with whitespace-only magic number
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "   ");
        request.setQueryStringParameters(queryParams);
        
        // Execute handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Verify error response
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertTrue(responseBody.get("error").asText().contains("cannot be empty"));
    }

    @Test
    void testNegativeMagicNumber() throws Exception {
        // Create request with negative magic number
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "-10");
        request.setQueryStringParameters(queryParams);
        
        // Execute handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Verify response (negative numbers should be valid)
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(-10, responseBody.get("magicNumber").asInt());
        assertEquals(true, responseBody.get("isEven").asBoolean());
        assertEquals(100, responseBody.get("squared").asInt());
    }

    @Test
    void testOptionsRequest() throws Exception {
        // Create OPTIONS request for CORS preflight
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("OPTIONS");
        
        // Execute handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Verify response
        assertEquals(200, response.getStatusCode());
        assertEquals("{}", response.getBody());
        
        // Verify CORS headers
        assertNotNull(response.getHeaders());
        assertTrue(response.getHeaders().containsKey("Access-Control-Allow-Origin"));
    }

    @Test
    void testLargeValidNumber() throws Exception {
        // Create request with large valid number
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "999999");
        request.setQueryStringParameters(queryParams);
        
        // Execute handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Verify response
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(999999, responseBody.get("magicNumber").asInt());
    }

    @Test
    void testZeroMagicNumber() throws Exception {
        // Create request with zero
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("magicNumber", "0");
        request.setQueryStringParameters(queryParams);
        
        // Execute handler
        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
        
        // Verify response
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertEquals(0, responseBody.get("magicNumber").asInt());
        assertEquals(true, responseBody.get("isEven").asBoolean());
        assertEquals(0, responseBody.get("squared").asInt());
    }
}
