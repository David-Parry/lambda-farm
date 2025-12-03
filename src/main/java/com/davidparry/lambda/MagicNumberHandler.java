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
            
            // Parse and validate the magic number parameter
            Integer magicNumber = parseMagicNumber(queryParams, context);

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
            
        } catch (IllegalArgumentException e) {
            // Client error - invalid input
            context.getLogger().log("Validation error: " + e.getMessage());
            return createErrorResponse(400, e.getMessage());
        } catch (Exception e) {
            // Server error - unexpected exception
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            context.getLogger().log("ERROR: " + e.getMessage() + "\n" + sw);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Parses and validates the magicNumber query parameter
     * 
     * @param queryParams The query string parameters from the request
     * @param context The Lambda context for logging
     * @return The parsed integer value
     * @throws IllegalArgumentException if the parameter is missing, empty, or not a valid integer
     */
    private Integer parseMagicNumber(Map<String, String> queryParams, Context context) {
        // Check if query parameters exist
        if (queryParams == null || queryParams.isEmpty()) {
            throw new IllegalArgumentException("Missing query parameters. The 'magicNumber' parameter is required.");
        }
        
        // Check if magicNumber parameter exists
        String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
        if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing or empty 'magicNumber' parameter. Please provide a valid integer.");
        }
        
        context.getLogger().log("Received magic number: " + magicNumberStr);
        
        // Validate format using regex - only accept valid integers (with optional leading minus sign)
        if (!magicNumberStr.matches("^-?\\d+$")) {
            throw new IllegalArgumentException(
                "Invalid 'magicNumber' parameter: '" + magicNumberStr + "'. " +
                "The magicNumber must be a valid integer (e.g., 42, -10, 0)."
            );
        }
        
        // Parse the integer
        try {
            return Integer.parseInt(magicNumberStr);
        } catch (NumberFormatException e) {
            // This should rarely happen due to regex validation, but handle overflow cases
            throw new IllegalArgumentException(
                "Invalid 'magicNumber' parameter: '" + magicNumberStr + "'. " +
                "The value is outside the valid integer range (-2147483648 to 2147483647)."
            );
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
}
