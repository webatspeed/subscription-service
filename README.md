# Subscription Service

### Mandatory Arguments

```bash
AWS_ACCESS_KEY      // an AWS SES access key
AWS_SECRET_KEY      // an AWS SES secret key
AWS_DEFAULT_REGION  // an AWS SES region, e.g.: eu-central-1
BUCKET_NAME         // name of bucket with attachment files
EMAIL               // default sender email address
```

### Optional Arguments

```bash
MAX_ERRORS  // number of errors till processing is suppressed, default: 3
CORS_ORIGIN // allowed request origin (pattern), default: http://localhost:3000
MONGO_HOST  // MongoDB host, default: localhost
MONGO_PORT  // MongoDB port, default: 27017
MONGO_DB    // MongoDB database name, default: subscription 
MONGO_USER  // MongoDB user name, default: user
MONGO_PASS  // MongoDB user password, default: pass
```
