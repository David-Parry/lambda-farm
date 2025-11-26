# Lambda Farm

Collection of AWS Lambda functions (Java 21) for various use cases.

## Lambda Functions

### 1. CloudWatch Logs Webhook Handler
Forwards CloudWatch Logs to a webhook endpoint in real-time.

### 2. Magic Number Handler
API Gateway integrated Lambda that processes a "magic number" query parameter from GET requests.
- **Handler**: `com.davidparry.lambda.MagicNumberHandler`
- **Setup IAM Role**: `./create-lambda-role.sh` (run once)
- **Deployment**: `./deploy-magic-number.sh`

---

## CloudWatch Logs Webhook Handler

## Build

```bash
./gradlew clean build shadowJar
```

Output: `build/libs/lambda-farm-1.0.0.jar`

