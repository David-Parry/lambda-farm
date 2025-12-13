package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MagicNumberHandlerTest {

    private MagicNumberHandler handler;
    private Context mockContext;
    private LambdaLogger mockLogger;

    @BeforeEach
    void setUp() {
        handler = new MagicNumberHandler();
        mockContext = mock(Context.class);
        mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Test
    @DisplayName("Should return 400 when query parameters are null")
    void testNullQueryParameters() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody(), "Response body should not be null");
        assertTrue(response.getBody().contains("Missing required query parameter"), 
            "Expected error message about missing query parameter, got: " + response.getBody());
    }

    @Test
    @DisplayName("Should return 400 when magicNumber parameter is missing")
    void testMissingMagicNumberParameter() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(new HashMap<>());

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody(), "Response body should not be null");
        System.out.println("Response body: " + response.getBody());
        assertTrue(response.getBody().contains("Missing") || response.getBody().contains("empty"), 
            "Expected error message about missing parameter, got: " + response.getBody());
    }

    @Test
    @DisplayName("Should return 400 when magicNumber contains non-numeric characters")
    void testInvalidMagicNumberFormat() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> params = new HashMap<>();
        params.put("magicNumber", "123abcert");
        request.setQueryStringParameters(params);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid magicNumber format"));
    }

    @Test
    @DisplayName("Should return 200 with valid numeric magicNumber")
    void testValidMagicNumber() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> params = new HashMap<>();
        params.put("magicNumber", "42");
        request.setQueryStringParameters(params);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"magicNumber\":42"));
    }

    @Test
    @DisplayName("Should handle negative numbers correctly")
    void testNegativeMagicNumber() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> params = new HashMap<>();
        params.put("magicNumber", "-10");
        request.setQueryStringParameters(params);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"magicNumber\":-10"));
    }

    @Test
    @DisplayName("Should return 400 for empty magicNumber value")
    void testEmptyMagicNumber() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        Map<String, String> params = new HashMap<>();
        params.put("magicNumber", "");
        request.setQueryStringParameters(params);

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
    }

    @Test
    @DisplayName("Should return 200 for OPTIONS preflight request")
    void testOptionsRequest() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("OPTIONS");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
    }
}
