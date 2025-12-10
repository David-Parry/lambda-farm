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
            // Validate query parameters exist
            Map<String, String> queryParams = request.getQueryStringParameters();
            if (queryParams == null) {
                context.getLogger().log("WARN: No query parameters provided");
                return createErrorResponse(400, "Missing query parameters. Please provide 'magicNumber' parameter.");
            }
            
            // Validate magicNumber parameter exists
            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
                context.getLogger().log("WARN: Missing magicNumber parameter");
                return createErrorResponse(400, "Missing required parameter 'magicNumber'. Please provide a valid integer.");
            }
            
            context.getLogger().log("Received magic number: " + magicNumberStr);
            
            // Validate and parse the magic number with proper error handling
            Integer magicNumber;
            try {
                magicNumber = parseMagicNumber(magicNumberStr);
            } catch (IllegalArgumentException e) {
                context.getLogger().log("WARN: Invalid magicNumber format: " + magicNumberStr);
                return createErrorResponse(400, "Invalid magicNumber provided: '" + magicNumberStr + "'. Please provide a valid integer.");
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
     * Safely parses a string to an integer with validation
     * 
     * @param value the string value to parse
     * @return the parsed integer
     * @throws IllegalArgumentException if the value is not a valid integer
     */
    private Integer parseMagicNumber(String value) throws IllegalArgumentException {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Magic number cannot be null or empty");
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer format: " + value, e);
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
