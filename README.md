# Lambda Farm

Collection of AWS Lambda functions (Java 25) for various use cases for Demo only.

## Lambda Functions

### 1. CloudWatch Logs Webhook Handler
Forwards CloudWatch Logs to a webhook endpoint in real-time.

### 2. Magic Number Handler
API Gateway integrated Lambda that processes a "magic number" query parameter from GET requests.
- **Handler**: `com.davidparry.lambda.MagicNumberHandler`

---

## CloudWatch Logs Webhook Handler

## Build

```bash
./gradlew clean build shadowJar
```

Output: `build/libs/lambda-farm-1.0.0.jar`

