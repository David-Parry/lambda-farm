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
            
            // Validate that query parameters exist
            if (queryParams == null || queryParams.isEmpty()) {
                context.getLogger().log("ERROR: No query parameters provided");
                return createBadRequestResponse("Missing required query parameter: " + MAGIC_NUMBER_PARAM);
            }

            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            
            // Validate that magicNumber parameter exists
            if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
                context.getLogger().log("ERROR: magicNumber parameter is missing or empty");
                return createBadRequestResponse("Missing required query parameter: " + MAGIC_NUMBER_PARAM);
            }
            
            // Validate that magicNumber contains only numeric characters (with optional leading +/-)
            if (!isValidInteger(magicNumberStr)) {
                context.getLogger().log("ERROR: Invalid magicNumber format: " + magicNumberStr);
                return createBadRequestResponse("Invalid magicNumber format. Expected a valid integer, got: " + magicNumberStr);
            }
            
            context.getLogger().log("Received magic number: " + magicNumberStr);
            
            // Parse the magic number (safe now after validation)
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
            // This should not happen after validation, but keeping as safety net
            context.getLogger().log("ERROR: NumberFormatException - " + e.getMessage());
            return createBadRequestResponse("Invalid magicNumber format. Must be a valid integer.");
        } catch (Exception e) {
            // Log server errors with stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            context.getLogger().log("ERROR: " + e.getMessage() + "\n" + sw);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Validates that a string represents a valid integer
     * Accepts optional leading +/- sign followed by digits
     * 
     * @param value the string to validate
     * @return true if the string is a valid integer format, false otherwise
     */
    private boolean isValidInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = value.trim();
        
        // Check if it matches the pattern for a valid integer: optional +/- followed by digits
        return trimmed.matches("^[+-]?\\d+$");
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
     * Creates a bad request (400) response for client errors
     * 
     * @param message the error message to include in the response
     * @return APIGatewayProxyResponseEvent with 400 status code
     */
    private APIGatewayProxyResponseEvent createBadRequestResponse(String message) {
        return createErrorResponse(400, message);
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
