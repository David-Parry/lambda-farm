package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Lambda handler for API Gateway GET requests that processes a "magic number" query parameter
 */
public class MagicNumberHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MAGIC_NUMBER_PARAM = "magicNumber";
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Received API Gateway request");
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(createHeaders());
        
        // Handle OPTIONS preflight request for CORS
        if ("OPTIONS".equals(request.getHttpMethod())) {
            response.setStatusCode(200);
            response.setBody("{}");
            return response;
        }
        
        try {
            Map<String, String> queryParams = request.getQueryStringParameters();
            
            // Validate query parameters exist
            if (queryParams == null || !queryParams.containsKey(MAGIC_NUMBER_PARAM)) {
                context.getLogger().log("WARN: Missing required query parameter 'magicNumber'");
                return createValidationErrorResponse(400, "MISSING_PARAMETER", 
                    "Required query parameter 'magicNumber' is missing", null);
            }

            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            context.getLogger().log("Received magic number: " + magicNumberStr);
            
            // Validate the magic number format
            if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
                context.getLogger().log("WARN: Empty magic number parameter received");
                return createValidationErrorResponse(400, "EMPTY_PARAMETER", 
                    "Query parameter 'magicNumber' cannot be empty", magicNumberStr);
            }
            
            // Validate and parse the magic number
            Integer magicNumber = parseMagicNumber(magicNumberStr, context);
            if (magicNumber == null) {
                context.getLogger().log("WARN: Invalid magic number received: '" + magicNumberStr + "'");
                return createValidationErrorResponse(400, "INVALID_MAGIC_NUMBER", 
                    "Query parameter 'magicNumber' must be a valid integer", magicNumberStr);
            }

            // Process the magic number (example logic)
            ObjectNode responseBody = objectMapper.createObjectNode();
            responseBody.put("magicNumber", magicNumber);
            responseBody.put("isEven", magicNumber % 2 == 0);
            responseBody.put("squared", magicNumber * magicNumber);
            responseBody.put("message", "Successfully processed magic number: " + magicNumber);
            
            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(responseBody));
            
            context.getLogger().log("Successfully processed magic number: " + magicNumber);
            return response;
            
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            context.getLogger().log("ERROR: " + e.getMessage() + "\n" + sw);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Validates and parses the magic number string
     * @param magicNumberStr the string to parse
     * @param context the Lambda context for logging
     * @return the parsed Integer, or null if invalid
     */
    private Integer parseMagicNumber(String magicNumberStr, Context context) {
        // Trim whitespace
        String trimmed = magicNumberStr.trim();
        
        // Validate format using regex (optional sign followed by digits)
        if (!trimmed.matches("-?\\d+")) {
            return null;
        }
        
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            // This should not happen if regex validation passed, but handle defensively
            context.getLogger().log("WARN: NumberFormatException despite regex validation: " + trimmed);
            return null;
        }
    }
    
    /**
     * Creates standard CORS headers for the response
     */
    private Map<String, String> createHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, x-api-key");
        return headers;
    }
    
    /**
     * Creates an error response with the given status code and message
     */
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(createHeaders());
        
        try {
            ObjectNode errorBody = objectMapper.createObjectNode();
            errorBody.put("error", message);
            errorBody.put("statusCode", statusCode);
            response.setBody(objectMapper.writeValueAsString(errorBody));
        } catch (Exception e) {
            response.setBody("{\"error\":\"" + message + "\",\"statusCode\":" + statusCode + "}");
        }
        
        return response;
    }
    
    /**
     * Creates a validation error response with detailed information
     */
    private APIGatewayProxyResponseEvent createValidationErrorResponse(int statusCode, String code, 
                                                                        String message, String receivedValue) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(createHeaders());
        
        try {
            ObjectNode errorBody = objectMapper.createObjectNode();
            errorBody.put("error", message);
            errorBody.put("code", code);
            errorBody.put("statusCode", statusCode);
            if (receivedValue != null) {
                errorBody.put("receivedValue", receivedValue);
            }
            response.setBody(objectMapper.writeValueAsString(errorBody));
        } catch (Exception e) {
            response.setBody("{\"error\":\"" + message + "\",\"code\":\"" + code + 
                           "\",\"statusCode\":" + statusCode + "}");
        }
        
        return response;
    }
}
