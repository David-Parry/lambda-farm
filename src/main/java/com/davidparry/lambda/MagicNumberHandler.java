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
        
        try {
            Map<String, String> queryParams = request.getQueryStringParameters();
            
            // Validate query parameters exist
            if (queryParams == null) {
                context.getLogger().log("Validation error: No query parameters provided");
                return createErrorResponse(400, "Query parameter 'magicNumber' is required and must be a valid integer.");
            }

            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            context.getLogger().log("Received magic number: " + magicNumberStr);
            
            // Validate and parse the magic number with proper error handling
            Integer magicNumber;
            try {
                magicNumber = parseAndValidateMagicNumber(magicNumberStr);
            } catch (InvalidMagicNumberException e) {
                context.getLogger().log("Validation error for magicNumber: " + magicNumberStr + " - " + e.getMessage());
                return createErrorResponse(400, e.getMessage());
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
     * Validates and parses the magic number query parameter
     * 
     * @param magicNumberStr the raw query parameter value
     * @return parsed Integer value
     * @throws InvalidMagicNumberException if the value is null, empty, or not a valid integer
     */
    private Integer parseAndValidateMagicNumber(String magicNumberStr) throws InvalidMagicNumberException {
        // Check for null or empty value
        if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
            throw new InvalidMagicNumberException("Query parameter 'magicNumber' is required and must be a valid integer.");
        }
        
        // Attempt to parse as integer
        try {
            return Integer.parseInt(magicNumberStr.trim());
        } catch (NumberFormatException e) {
            throw new InvalidMagicNumberException("Query parameter 'magicNumber' must be a valid integer. Received: '" + magicNumberStr + "'");
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
        headers.put("Access-Control-Allow-Headers", "Content-Type");
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
     * Custom exception for invalid magic number input
     */
    private static class InvalidMagicNumberException extends Exception {
        public InvalidMagicNumberException(String message) {
            super(message);
        }
    }
}
