# Subscription Service

### Mandatory Arguments

```bash
SES_USER  // an AWS SES access key
SES_PASS  // an AWS SES secret key
EMAIL     // default sender email address
```

### Optional Arguments

```bash
SES_REGION  // an AWS SES region, default: eu-central-1
MAX_ERRORS  // number of errors until processing is suppressed, default: 3
CORS_ORIGIN // allowed request origin (pattern), default: "http://localhost:3000"
MONGO_HOST  // MongoDB host, default: localhost
MONGO_PORT  // MongoDB port, default: 27017
MONGO_DB    // MongoDB database name, default: subscription 
MONGO_USER  // MongoDB user name, default: user
MONGO_PASS  // MongoDB user password, default: pass
```
