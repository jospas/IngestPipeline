## Amazon Serverless Ingest Pipeline

Proof of concept serverless ingest pipeline which aims to listen for input manaifest files created in S3 and process a batch of CSV files using a Java based Lambda functions.

Deployment occurs using AWS SAM and AWS CloudFormation.

### Getting Started

The system uses Apache Maven and is easily configured in IntelliJ by importing the Maven pom.xml as a new project.

### Prerequisites

To test this from a local development machine:

<!--* Create or identify an S3 bucket in your account to upload Lambda code to
* Create an IAM user and add to a group
* Configure a local named profile in the AWS CLI
* Grant the group IAM permissions for:
    * S3 write to the bucket
    * Kinesis
    * SNS
    * Lambda
    * CloudWatch
    * Creation of IAM roles-->

### Building

Build either from within IntelliJ or use Maven:

```bash
mvn package
```

### Installing

#### Infrastructure Template

This template creates two S3 buckets and SNS topics that listen to object creation events.

Use the AWS CLI to deploy via CloudFormation:

```bash
aws cloudformation deploy \
    --template-file cloudformation/Infrastructure.yaml \
    --profile <aws profile> \
    --region <aws region> \
    --stack-name <stack name> \
    --parameter-overrides Stage=<stage> SourceSystem=<source system name>
```

For example deploying a dev stack for MyTestSystem:

```bash
aws cloudformation deploy \
    --template-file cloudformation/Infrastructure.yaml \
    --profile aws-josh \
    --region ap-southeast-2 \
    --stack-name MyTestSystemStack \
    --parameter-overrides Stage=dev SourceSystemName=mytestsystem
```