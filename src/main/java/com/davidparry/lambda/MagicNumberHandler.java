package com.davidparry.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Lambda handler for API Gateway GET requests that processes a "magic number" query parameter
 */
public class MagicNumberHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private static final Logger log = LogManager.getLogger(MagicNumberHandler.class);
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
            // Validate query parameters map is not null
            Map<String, String> queryParams = request.getQueryStringParameters();
            if (queryParams == null) {
                log.warn("Request received with null query parameters");
                return createErrorResponse(400, "MISSING_PARAMETERS", 
                    "Missing query parameters. Please provide a 'magicNumber' parameter.");
            }
            
            // Extract and validate magicNumber parameter
            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            
            if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
                log.warn("Request received with missing or empty magicNumber parameter");
                return createErrorResponse(400, "MISSING_MAGIC_NUMBER", 
                    "The 'magicNumber' parameter is required and cannot be empty.");
            }
            
            context.getLogger().log("Received magic number: " + magicNumberStr);
            
            // Validate numeric format before parsing
            if (!isValidInteger(magicNumberStr)) {
                log.warn("Invalid magicNumber format received: {}", magicNumberStr);
                return createErrorResponse(400, "INVALID_MAGIC_NUMBER", 
                    "The 'magicNumber' parameter must be a valid integer. Received: " + magicNumberStr);
            }
            
            // Safe parsing with try-catch as secondary safety net
            Integer magicNumber;
            try {
                magicNumber = Integer.parseInt(magicNumberStr.trim());
            } catch (NumberFormatException e) {
                log.warn("NumberFormatException parsing magicNumber: {}", magicNumberStr, e);
                return createErrorResponse(400, "INVALID_MAGIC_NUMBER", 
                    "The 'magicNumber' parameter must be a valid integer. Received: " + magicNumberStr);
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
            log.info("Successfully processed magic number: {}", magicNumber);
            return response;
            
        } catch (Exception e) {
            // Log unexpected server errors at ERROR level
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            context.getLogger().log("ERROR: " + e.getMessage() + "\n" + sw);
            log.error("Unexpected error processing request", e);
            return createErrorResponse(500, "INTERNAL_ERROR", "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Validates if a string represents a valid integer
     * @param str the string to validate
     * @return true if the string is a valid integer, false otherwise
     */
    private boolean isValidInteger(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = str.trim();
        
        // Check for valid integer pattern (optional minus sign followed by digits)
        return trimmed.matches("-?\\d+");
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
     * Creates an error response with the given status code, error code, and message
     */
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String errorCode, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(createHeaders());
        
        try {
            ObjectNode errorBody = objectMapper.createObjectNode();
            errorBody.put("errorCode", errorCode);
            errorBody.put("message", message);
            errorBody.put("statusCode", statusCode);
            response.setBody(objectMapper.writeValueAsString(errorBody));
        } catch (Exception e) {
            response.setBody("{\"errorCode\":\"" + errorCode + "\",\"message\":\"" + message + "\",\"statusCode\":" + statusCode + "}");
        }
        
        return response;
    }
}
