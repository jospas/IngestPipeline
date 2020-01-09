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

The templates should be deployed in the order detailed below.

#### 1) Infrastructure Template

This template creates two S3 buckets and SNS topics that listen to object creation events.

Use the AWS CLI to deploy via CloudFormation:

```bash
aws cloudformation deploy \
    --template-file cloudformation/Infrastructure.yaml \
    --profile <aws profile> \
    --region <aws region> \
    --stack-name <stack name> \
    --parameter-overrides Stage=<stage> \
    	SourceSystemName=<source system name>
```

For example deploying a dev stack for MyTestSystem:

```bash
aws cloudformation deploy \
    --template-file cloudformation/Infrastructure.yaml \
    --profile aws-josh \
    --region ap-southeast-2 \
    --stack-name MyTestSystemStack \
    --parameter-overrides Stage=dev \
    	SourceSystemName=mytestsystem
```

#### 2) Lambda Roles Template

This template creates IAM roles for the two Lambda functions, subscribes them to an SQS queue and subscribes the SQS queuer to the SNS topics attached to the infrastrcuture buckets.

It requires the capabilties for named IAM changes.

Use the AWS CLI to deploy via CloudFormation:

```bash
aws cloudformation deploy \
	--template-file cloudformation/LambdaRoles.yaml \
	--profile <aws profile> \
	--region <aws region> \
	--stack-name <stack name> \
	--parameter-overrides StageName=<stage> \
		SourceSystemName=<source system name> \
	--capabilities CAPABILITY_NAMED_IAM
```

For example deploying a dev stack for MyTestSystem:

```bash
aws cloudformation deploy \
	--template-file cloudformation/LambdaRoles.yaml \
	--profile aws-josh \
	--region ap-southeast-2 \
	--stack-name MyTestSystemIAMStack \
	--parameter-overrides StageName=dev \
		SourceSystemName=mytestsystem \
	--capabilities CAPABILITY_NAMED_IAM
```

#### 3) Input Lambda Template

This template deploys the SQS and Lambda processing pipeline subscribed the input S3 bucket change notifications via SNS.

It requires the Lambda roles created in step 2).

Prepare the stack for deployment via CloudFormation:

```bash
aws cloudformation package \
	--template-file cloudformation/InputLambda.yaml \
	--output-template-file cloudformation/InputLambda-prepared.yaml \
	--profile <aws profile> \
    --s3-bucket <s3 bucket> \
    --s3-prefix <s3 key prefix> \	
	--region <aws region> \
	--force-upload
```

For example preparing a dev stack for MyTestSystem:

```bash
aws cloudformation package \
	--template-file cloudformation/InputLambda.yaml \
	--output-template-file cloudformation/InputLambda-prepared.yaml \
	--profile aws-josh \
    --s3-bucket aws-joshpas-home \
    --s3-prefix applications/inputlambda/ \
	--region ap-southeast-2 \
	--force-upload
```

Now deploy the prepared template:

```bash
aws cloudformation deploy \
	--template-file cloudformation/InputLambda-prepared.yaml \
	--profile <aws profile> \
	--region <aws region> \
	--stack-name <stack name> \
	--parameter-overrides StageName=<stage> \
		SourceSystemName=<source system name>
```

For example deploying a dev stack for MyTestSystem:

```bash
aws cloudformation deploy \
	--template-file cloudformation/InputLambda-prepared.yaml \
	--profile aws-josh \
	--region ap-southeast-2 \
	--stack-name MyTestSystemInputLambdaStack \
	--parameter-overrides StageName=dev \
		SourceSystemName=mytestsystem
```