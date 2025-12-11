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

/**
 * Lambda handler for API Gateway GET requests that processes a "magic number" query parameter
 */
public class MagicNumberHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MAGIC_NUMBER_PARAM = "magicNumber";
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Received API Gateway request - Method: " + request.getHttpMethod());
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(createHeaders());
        
        // Handle OPTIONS preflight request for CORS
        if ("OPTIONS".equals(request.getHttpMethod())) {
            response.setStatusCode(200);
            response.setBody("{}");
            return response;
        }
        
        try {
            // Safely handle null query parameters
            Map<String, String> queryParams = request.getQueryStringParameters();
            if (queryParams == null) {
                queryParams = Collections.emptyMap();
            }
            
            context.getLogger().log("Query parameters: " + queryParams);
            
            // Validate magic number parameter
            ValidationResult validationResult = validateMagicNumber(queryParams);
            if (!validationResult.isValid()) {
                context.getLogger().log("Validation failed: " + validationResult.getErrorMessage());
                return createErrorResponse(400, validationResult.getErrorMessage());
            }
            
            Integer magicNumber = validationResult.getValue();
            context.getLogger().log("Received valid magic number: " + magicNumber);
            
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
            // This catch block now only handles unexpected server errors
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            context.getLogger().log("ERROR: Unexpected server error - " + e.getMessage() + "\n" + sw);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Validates the magic number parameter from query string
     * 
     * @param queryParams the query string parameters map
     * @return ValidationResult containing the parsed value or error message
     */
    private ValidationResult validateMagicNumber(Map<String, String> queryParams) {
        // Check if parameter is present
        if (!queryParams.containsKey(MAGIC_NUMBER_PARAM)) {
            return ValidationResult.error("Missing required query parameter: " + MAGIC_NUMBER_PARAM);
        }
        
        String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
        
        // Check if parameter is null or empty
        if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
            return ValidationResult.error("Query parameter '" + MAGIC_NUMBER_PARAM + "' cannot be empty");
        }
        
        // Validate numeric format
        try {
            Integer magicNumber = Integer.parseInt(magicNumberStr.trim());
            return ValidationResult.success(magicNumber);
        } catch (NumberFormatException e) {
            return ValidationResult.error("Query parameter '" + MAGIC_NUMBER_PARAM + "' must be a valid integer. Received: " + magicNumberStr);
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
     * Helper class to encapsulate validation results
     */
    private static class ValidationResult {
        private final boolean valid;
        private final Integer value;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, Integer value, String errorMessage) {
            this.valid = valid;
            this.value = value;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success(Integer value) {
            return new ValidationResult(true, value, null);
        }
        
        public static ValidationResult error(String errorMessage) {
            return new ValidationResult(false, null, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public Integer getValue() {
            return value;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
