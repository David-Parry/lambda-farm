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
            
            // Validate query parameters are present
            if (queryParams == null || !queryParams.containsKey(MAGIC_NUMBER_PARAM)) {
                context.getLogger().log("CLIENT_ERROR: Missing required query parameter: " + MAGIC_NUMBER_PARAM);
                return createErrorResponse(400, "Missing required query parameter: " + MAGIC_NUMBER_PARAM);
            }

            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            context.getLogger().log("Received magic number: " + magicNumberStr);
            
            // Validate the magic number parameter is not null or empty
            if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
                context.getLogger().log("CLIENT_ERROR: Empty magicNumber parameter");
                return createErrorResponse(400, "Invalid magicNumber parameter. It must be a non-empty integer.");
            }
            
            // Validate the magic number parameter is numeric
            if (!isValidInteger(magicNumberStr)) {
                context.getLogger().log("CLIENT_ERROR: Invalid magicNumber parameter: '" + magicNumberStr + "'");
                return createErrorResponse(400, "Invalid magicNumber parameter. It must be an integer.");
            }
            
            // Parse the magic number (safe after validation)
            Integer magicNumber = Integer.parseInt(magicNumberStr);

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
            
        } catch (NumberFormatException e) {
            // This should not happen after validation, but handle it as a client error
            context.getLogger().log("CLIENT_ERROR: NumberFormatException - " + e.getMessage());
            return createErrorResponse(400, "Invalid magicNumber parameter. It must be an integer.");
        } catch (Exception e) {
            // True server errors (e.g., serialization issues)
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            context.getLogger().log("ERROR: " + e.getMessage() + "\n" + sw);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Validates that a string represents a valid integer
     * @param value the string to validate
     * @return true if the string is a valid integer, false otherwise
     */
    private boolean isValidInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        
        // Check for optional leading sign
        int startIndex = 0;
        if (trimmed.charAt(0) == '-' || trimmed.charAt(0) == '+') {
            if (trimmed.length() == 1) {
                return false; // Just a sign with no digits
            }
            startIndex = 1;
        }
        
        // Check that all remaining characters are digits
        for (int i = startIndex; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        
        return true;
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
}
