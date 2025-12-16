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

    // Maximum absolute input value for which squaring fits in a signed 32-bit integer.
    private static final int MAX_SAFE_SQUARE_INPUT = 46340;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        if (context != null && context.getLogger() != null) {
            context.getLogger().log("Received API Gateway request");
        }

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(createHeaders());

        if (request == null) {
            logWarn(context, "Missing request");
            return badRequest("Missing request");
        }

        // Handle OPTIONS preflight request for CORS
        if ("OPTIONS".equals(request.getHttpMethod())) {
            response.setStatusCode(200);
            response.setBody("{}");
            return response;
        }

        try {
            Map<String, String> queryParams = request.getQueryStringParameters();

            if (queryParams == null) {
                logWarn(context, "Missing query parameters");
                return badRequest("Missing query parameters");
            }

            String magicNumberStr = queryParams.get(MAGIC_NUMBER_PARAM);
            if (magicNumberStr == null || magicNumberStr.isBlank()) {
                logWarn(context, "Missing required query parameter: " + MAGIC_NUMBER_PARAM);
                return badRequest("Missing required query parameter: " + MAGIC_NUMBER_PARAM);
            }

            if (context != null && context.getLogger() != null) {
                context.getLogger().log("Received magic number: " + magicNumberStr);
            }

            Integer magicNumber;
            try {
                magicNumber = Integer.parseInt(magicNumberStr);
            } catch (NumberFormatException e) {
                logWarn(context, "magicNumber must be a valid integer");
                return badRequest("magicNumber must be a valid integer");
            }

            if (magicNumber < -MAX_SAFE_SQUARE_INPUT || magicNumber > MAX_SAFE_SQUARE_INPUT) {
                logWarn(context, "magicNumber out of range (must be between -46340 and 46340)");
                return badRequest("magicNumber out of range (must be between -46340 and 46340)");
            }

            // Process the magic number (example logic)
            ObjectNode responseBody = objectMapper.createObjectNode();
            responseBody.put("magicNumber", magicNumber);
            responseBody.put("isEven", magicNumber % 2 == 0);
            responseBody.put("squared", magicNumber * magicNumber);
            responseBody.put("message", "Successfully processed magic number: " + magicNumber);

            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(responseBody));

            if (context != null && context.getLogger() != null) {
                context.getLogger().log("Successfully processed magic number: " + magicNumber);
            }
            return response;

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            if (context != null && context.getLogger() != null) {
                context.getLogger().log("ERROR: " + e.getMessage() + "\n" + sw);
            }
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent badRequest(String message) {
        return createErrorResponse(400, message);
    }

    private void logWarn(Context context, String message) {
        if (context == null || context.getLogger() == null) {
            return;
        }

        context.getLogger().log("WARN: " + message);
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
