package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Lambda handler for API Gateway GET requests that processes a "magic number" query parameter
 */
public class MagicNumberHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MAGIC_NUMBER_PARAM = "magicNumber";
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    
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
            // Safely access query parameters with null protection
            Map<String, String> queryParams = Objects.requireNonNullElse(
                request.getQueryStringParameters(), 
                Collections.emptyMap()
            );
            
            // Validate that magicNumber parameter exists
            if (!queryParams.containsKey(MAGIC_NUMBER_PARAM)) {
                context.getLogger().log("INFO: Missing magicNumber query parameter");
                return createErrorResponse(400, "Missing required query parameter: " + MAGIC_NUMBER_PARAM);
            }

            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            context.getLogger().log("Received magic number: " + magicNumberStr);
            
            // Validate and parse the magic number
            Integer magicNumber = validateAndParseMagicNumber(magicNumberStr, context);

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
            // Handle validation errors with 400 Bad Request
            context.getLogger().log("INFO: Validation error - " + e.getMessage());
            return createErrorResponse(400, e.getMessage());
        } catch (Exception e) {
            // Handle unexpected errors with 500 Internal Server Error
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            context.getLogger().log("ERROR: " + e.getMessage() + "\n" + sw);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Validates and parses the magic number string
     * 
     * @param value the string value to validate and parse
     * @param context the Lambda context for logging
     * @return the parsed integer value
     * @throws IllegalArgumentException if the value is null, empty, or not a valid integer
     */
    private Integer validateAndParseMagicNumber(String value, Context context) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("magicNumber parameter cannot be empty");
        }
        
        // Validate that the string contains only digits (and optional leading minus sign)
        if (!INTEGER_PATTERN.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(
                "Invalid magicNumber format: '" + value + "'. Must be a valid integer."
            );
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // This should rarely happen due to regex validation, but handle it just in case
            throw new IllegalArgumentException(
                "magicNumber value out of range or invalid: '" + value + "'"
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
