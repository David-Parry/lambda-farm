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
            // Safely handle null queryStringParameters
            Map<String, String> queryParams = request.getQueryStringParameters();
            if (queryParams == null) {
                queryParams = Collections.emptyMap();
            }
            
            // Validate that magicNumber parameter exists
            if (!queryParams.containsKey(MAGIC_NUMBER_PARAM)) {
                context.getLogger().log("WARN: Missing required parameter: " + MAGIC_NUMBER_PARAM);
                return createErrorResponse(400, "Missing required query parameter: " + MAGIC_NUMBER_PARAM);
            }

            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            context.getLogger().log("Received magic number: " + magicNumberStr);
            
            // Validate that magicNumber is not null or empty
            if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
                context.getLogger().log("WARN: Empty magicNumber parameter");
                return createErrorResponse(400, "Query parameter 'magicNumber' cannot be empty");
            }
            
            // Validate that magicNumber is a valid integer format
            if (!isValidInteger(magicNumberStr.trim())) {
                context.getLogger().log("WARN: Invalid magicNumber format: " + magicNumberStr);
                return createErrorResponse(400, "Query parameter 'magicNumber' must be a valid integer");
            }
            
            // Parse the magic number (safe now after validation)
            Integer magicNumber;
            try {
                magicNumber = Integer.parseInt(magicNumberStr.trim());
            } catch (NumberFormatException e) {
                context.getLogger().log("WARN: NumberFormatException for input: " + magicNumberStr);
                return createErrorResponse(400, "Query parameter 'magicNumber' must be a valid integer");
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
            context.getLogger().log("ERROR: Unexpected server error: " + e.getMessage() + "\n" + sw);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Validates if a string represents a valid integer (including negative numbers)
     * @param str the string to validate
     * @return true if the string is a valid integer format, false otherwise
     */
    private boolean isValidInteger(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        // Regex pattern for valid integers: optional minus sign followed by digits
        return str.matches("^-?\\d+$");
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
