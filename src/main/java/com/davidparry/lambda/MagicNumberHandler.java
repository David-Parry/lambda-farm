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
import java.util.regex.Pattern;

/**
 * Lambda handler for API Gateway GET requests that processes a "magic number" query parameter
 */
public class MagicNumberHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MAGIC_NUMBER_PARAM = "magicNumber";
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?\\d+$");
    
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
            // Null-safe retrieval of query parameters
            Map<String, String> queryParams = request.getQueryStringParameters();
            if (queryParams == null || queryParams.isEmpty()) {
                context.getLogger().log("Validation error: Missing query parameters");
                return createValidationErrorResponse("Missing required query parameter: " + MAGIC_NUMBER_PARAM);
            }
            
            // Null-safe retrieval of magic number parameter
            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
                context.getLogger().log("Validation error: Missing magicNumber parameter");
                return createValidationErrorResponse("Missing required query parameter: " + MAGIC_NUMBER_PARAM);
            }
            
            // Validate numeric format before parsing
            if (!NUMERIC_PATTERN.matcher(magicNumberStr.trim()).matches()) {
                context.getLogger().log("Validation error: Invalid format for magicNumber: " + magicNumberStr);
                return createValidationErrorResponse("Invalid format for magicNumber. Expected a valid integer, got: " + magicNumberStr);
            }
            
            context.getLogger().log("Received magic number: " + magicNumberStr);
            
            // Parse the magic number (safe after validation)
            Integer magicNumber = Integer.parseInt(magicNumberStr.trim());

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
            // This should not occur after validation, but handle defensively
            context.getLogger().log("NumberFormatException: " + e.getMessage());
            return createValidationErrorResponse("Invalid numeric format for magicNumber parameter");
        } catch (Exception e) {
            // Log full stack trace for server errors
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            context.getLogger().log("ERROR: " + e.getMessage() + "\n" + sw);
            
            // Return generic error message to client (don't expose internal details)
            return createErrorResponse(500, "Internal server error occurred while processing request");
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
     * Creates a validation error response (400 Bad Request)
     */
    private APIGatewayProxyResponseEvent createValidationErrorResponse(String message) {
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
