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
            // Validate and parse the magic number parameter
            Integer magicNumber = validateAndParseMagicNumber(request, context);
            
            // If validation failed, the method returns null and we should have already returned an error
            if (magicNumber == null) {
                context.getLogger().log("Validation failed - this should not happen");
                return createErrorResponse(500, "Internal validation error");
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
            
        } catch (NumberFormatException e) {
            context.getLogger().log("Invalid magic number format: " + e.getMessage());
            return createErrorResponse(400, "magicNumber must be a valid integer");
        } catch (IllegalArgumentException e) {
            context.getLogger().log("Validation error: " + e.getMessage());
            return createErrorResponse(400, e.getMessage());
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            context.getLogger().log("ERROR: " + e.getMessage() + "\n" + sw);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Validates and parses the magic number from the request query parameters
     * 
     * @param request The API Gateway request event
     * @param context The Lambda execution context
     * @return The parsed magic number as an Integer
     * @throws IllegalArgumentException if the parameter is missing or null
     * @throws NumberFormatException if the parameter is not a valid integer
     */
    private Integer validateAndParseMagicNumber(APIGatewayProxyRequestEvent request, Context context) {
        // Guard against null request
        if (request == null) {
            context.getLogger().log("Request is null");
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        // Guard against null query parameters map
        Map<String, String> queryParams = request.getQueryStringParameters();
        if (queryParams == null) {
            context.getLogger().log("Query parameters are missing");
            throw new IllegalArgumentException("Query parameter '" + MAGIC_NUMBER_PARAM + "' is required");
        }
        
        // Check if the magic number parameter exists
        String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
        if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
            context.getLogger().log("Magic number parameter is missing or empty");
            throw new IllegalArgumentException("Query parameter '" + MAGIC_NUMBER_PARAM + "' is required and cannot be empty");
        }
        
        context.getLogger().log("Received magic number: " + magicNumberStr);
        
        // Parse and validate the magic number
        try {
            return Integer.parseInt(magicNumberStr.trim());
        } catch (NumberFormatException e) {
            context.getLogger().log("Failed to parse magic number: " + magicNumberStr);
            throw new NumberFormatException("For input string: " + magicNumberStr);
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
