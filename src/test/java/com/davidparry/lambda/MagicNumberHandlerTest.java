package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MagicNumberHandlerTest {

    private MagicNumberHandler handler;
    private Context mockContext;

    @BeforeEach
    void setUp() {
        handler = new MagicNumberHandler();

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
            public CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public ClientContext getClientContext() {
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
    void returns400WhenRequestIsNull() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(null, mockContext);

        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Missing request"));
    }

    @Test
    void returns400WhenQueryParamsMissing() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Missing query parameters"));
    }

    @Test
    void returns400WhenMagicNumberMissing() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(new HashMap<>());

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Missing required query parameter: magicNumber"));
    }

    @Test
    void returns400WhenMagicNumberBlank() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(Map.of("magicNumber", "   "));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Missing required query parameter: magicNumber"));
    }

    @Test
    void returns400WhenMagicNumberIsNotInteger() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(Map.of("magicNumber", "123abcert"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("magicNumber must be a valid integer"));
    }

    @Test
    void returns400WhenMagicNumberOutOfRangeForSquaring() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(Map.of("magicNumber", "50000"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("magicNumber out of range"));
    }

    @Test
    void returns200WhenMagicNumberValid() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("GET");
        request.setQueryStringParameters(Map.of("magicNumber", "12"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"magicNumber\":12"));
        assertTrue(response.getBody().contains("\"isEven\":true"));
        assertTrue(response.getBody().contains("\"squared\":144"));
    }

    @Test
    void returns200ForOptionsPreflight() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setHttpMethod("OPTIONS");

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);

        assertEquals(200, response.getStatusCode());
        assertEquals("{}", response.getBody());
    }
}
