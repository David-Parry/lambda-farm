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
import java.util.Optional;

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
            // Null check for request
            if (request == null) {
                context.getLogger().log("ERROR: Request is null");
                return createBadRequestResponse("Request cannot be null");
            }
            
            // Null check for query parameters
            Map<String, String> queryParams = request.getQueryStringParameters();
            if (queryParams == null) {
                context.getLogger().log("ERROR: Query parameters are null");
                return createBadRequestResponse("Missing query parameters. Please provide '" + MAGIC_NUMBER_PARAM + "' parameter.");
            }
            
            // Parse and validate magic number
            Optional<Integer> magicNumberOpt = parseMagicNumber(queryParams, context);
            if (!magicNumberOpt.isPresent()) {
                // Error already logged in parseMagicNumber
                return createBadRequestResponse("Invalid or missing '" + MAGIC_NUMBER_PARAM + "' parameter. Please provide a valid integer value.");
            }
            
            Integer magicNumber = magicNumberOpt.get();
            context.getLogger().log("Successfully parsed magic number: " + magicNumber);
            
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
            context.getLogger().log("ERROR: Unexpected server error: " + e.getMessage() + "\n" + sw);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Parses and validates the magic number from query parameters
     * 
     * @param queryParams the query parameters map
     * @param context the Lambda context for logging
     * @return Optional containing the parsed integer, or empty if invalid
     */
    private Optional<Integer> parseMagicNumber(Map<String, String> queryParams, Context context) {
        if (queryParams == null) {
            context.getLogger().log("ERROR: Query parameters map is null");
            return Optional.empty();
        }
        
        String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
        
        // Check if parameter exists
        if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
            context.getLogger().log("ERROR: Missing '" + MAGIC_NUMBER_PARAM + "' parameter");
            return Optional.empty();
        }
        
        // Validate numeric format using regex
        if (!magicNumberStr.matches("^-?\\d+$")) {
            context.getLogger().log("ERROR: Invalid '" + MAGIC_NUMBER_PARAM + "' value: " + magicNumberStr + " (not a valid integer)");
            return Optional.empty();
        }
        
        try {
            Integer magicNumber = Integer.parseInt(magicNumberStr);
            return Optional.of(magicNumber);
        } catch (NumberFormatException e) {
            // This should rarely happen due to regex validation, but handle it anyway
            context.getLogger().log("ERROR: Failed to parse '" + MAGIC_NUMBER_PARAM + "': " + magicNumberStr + " - " + e.getMessage());
            return Optional.empty();
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
     * Creates a bad request (400) response with the given message
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
