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
            if (queryParams == null) {
                context.getLogger().log("INFO: Missing query parameters");
                return createErrorResponse(400, "Missing query parameters. Please provide 'magicNumber' parameter.");
            }
            
            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            context.getLogger().log("Received magic number parameter: " + magicNumberStr);
            
            // Parse and validate the magic number
            Integer magicNumber;
            try {
                magicNumber = parseMagicNumber(magicNumberStr);
            } catch (IllegalArgumentException e) {
                context.getLogger().log("INFO: Validation failed - " + e.getMessage());
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
     * Parses and validates the magic number parameter
     * 
     * @param magicNumberStr the raw query parameter value
     * @return the parsed integer value
     * @throws IllegalArgumentException if the parameter is null, empty, or not a valid integer
     */
    private Integer parseMagicNumber(String magicNumberStr) {
        // Check for null or empty
        if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing 'magicNumber' parameter. Please provide a valid integer.");
        }
        
        // Validate numeric format (allow optional leading +/- sign followed by digits)
        if (!magicNumberStr.matches("^[+-]?\\d+$")) {
            throw new IllegalArgumentException(
                "Invalid 'magicNumber' parameter: '" + magicNumberStr + "'. Must be a valid integer."
            );
        }
        
        // Parse the integer
        try {
            return Integer.parseInt(magicNumberStr);
        } catch (NumberFormatException e) {
            // This should rarely happen due to regex validation, but handle overflow cases
            throw new IllegalArgumentException(
                "Invalid 'magicNumber' parameter: '" + magicNumberStr + "'. Value is out of range for integer."
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
