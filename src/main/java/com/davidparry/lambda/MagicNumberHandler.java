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
            Map<String, String> queryParams = request.getQueryStringParameters();
            
            // Validate query parameters are present
            if (queryParams == null || queryParams.isEmpty()) {
                context.getLogger().log("Missing query parameters");
                return createErrorResponse(400, "magicNumber query parameter is required");
            }

            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            
            // Validate magic number parameter is present
            if (magicNumberStr == null || magicNumberStr.trim().isEmpty()) {
                context.getLogger().log("Missing magicNumber parameter");
                return createErrorResponse(400, "magicNumber query parameter is required");
            }
            
            context.getLogger().log("Received magic number: " + magicNumberStr);
            
            // Validate and parse the magic number
            Integer magicNumber = parseMagicNumber(magicNumberStr, context);
            if (magicNumber == null) {
                context.getLogger().log("Invalid magicNumber: " + magicNumberStr);
                return createErrorResponse(400, "magicNumber must be a valid integer");
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
     * Validates and parses the magic number string
     * @param raw the raw string value from query parameter
     * @param context the Lambda context for logging
     * @return the parsed Integer, or null if validation fails
     */
    private Integer parseMagicNumber(String raw, Context context) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = raw.trim();
        
        // Validate the string matches integer pattern (optional minus sign followed by digits)
        if (!INTEGER_PATTERN.matcher(trimmed).matches()) {
            context.getLogger().log("magicNumber does not match integer pattern: " + trimmed);
            return null;
        }
        
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            context.getLogger().log("Failed to parse magicNumber: " + trimmed + " - " + e.getMessage());
            return null;
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
