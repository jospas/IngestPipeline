## Amazon Serverless Ingest Pipeline

<a name="contents"></a>
### Contents

- [Introduction](#introduction)
- [Getting started](#getting-started)
- [Installation](#installation)	
	- Infrastructure template 	
	- Lambda roles template
	- Ingest Lambda template
- asddsads
- [Building secure manifests](#building-manifests)
- [Limitations](#limitations)

<a name="introduction"></a>
### Introduction

Proof of concept serverless ingest pipeline which aims to listen for input manifest files created in S3 and process a batch of CSV files using a Java based Lambda functions.

Hooks to add features to the processing pipeline to allow data enrichment and transformation are provided.

Deployment to an AWS account is achieved via AWS CloudFormation.

<a name="getting-started"></a>
### Getting Started <a href="#contents">^</a>

The system uses Apache Maven and is easily configured in IntelliJ by importing the Maven pom.xml as a new project.

### Building <a href="#contents">^</a>

Build either from within IntelliJ or use Maven:

```bash
mvn package
```
<a name="installation"></a>
### Installation <a href="#contents">^</a>

Deployment is achieved via AWS CloudFormation.

The templates should be deployed in the order detailed below.

#### 1) Infrastructure Template <a href="#installing">^</a>

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
    --stack-name mytestsystem-infrastructure \
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

A sample deployment script is provided:

	scripts/deploy_input_lambda.sh

To deploy by hand, repare the stack for deployment via CloudFormation:

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

#### 3) Processed Lambda Template

This template deploys the SQS and Lambda processing pipeline subscribed the processed S3 bucket change notifications via SNS.

It requires the Lambda roles created in step 2).

A sample deployment script is provided:

	scripts/deploy_processed_lambda.sh

To deploy by hand, prepare the stack for deployment via CloudFormation:

```bash
aws cloudformation package \
	--template-file cloudformation/ProcessedLambda.yaml \
	--output-template-file cloudformation/ProcessedLambda-prepared.yaml \
	--profile <aws profile> \
    --s3-bucket <s3 bucket> \
    --s3-prefix <s3 key prefix> \	
	--region <aws region> \
	--force-upload
```

For example preparing a dev stack for MyTestSystem:

```bash
aws cloudformation package \
	--template-file cloudformation/ProcessedLambda.yaml \
	--output-template-file cloudformation/ProcessedLambda-prepared.yaml \
	--profile aws-josh \
    --s3-bucket aws-joshpas-home \
    --s3-prefix applications/processedlambda/ \
	--region ap-southeast-2 \
	--force-upload
```

Now deploy the prepared template:

```bash
aws cloudformation deploy \
	--template-file cloudformation/ProcessedLambda-prepared.yaml \
	--profile <aws profile> \
	--region <aws region> \
	--stack-name <stack name> \
	--parameter-overrides StageName=<stage> \
		SourceSystemName=<source system name>
```

For example deploying a dev stack for MyTestSystem:

```bash
aws cloudformation deploy \
	--template-file cloudformation/ProcessedLambda-prepared.yaml \
	--profile aws-josh \
	--region ap-southeast-2 \
	--stack-name MyTestSystemProcessedLambdaStack \
	--parameter-overrides StageName=dev \
		SourceSystemName=mytestsystem
```

#### 4) Copying configuration files

Edit the script:

	scripts/deploy_input_config.sh 

to use your bucket and KMS key ids and deploy some sample configuration by running this script.

#### 5) Inject a sample manifest.json

Edit the script:

	scripts/deploy_input_manifest.sh 
	
to use your bucket and KMS key ids and deploy a sample manifest.json for testing.


<a name="building-manifests"></a>

### Building secure Manifests <a href="#contents">^</a>

The system makes use of digital signatures to verify sender data integrity and identify. 

This requires the sender to produce manifest files with a hash signed by the source system's private key, can be verified by the ingest pipeline using the public key.

Example code is provided in:
	
	Manifest.java - signManifest(String [] keys)
	
See: [Java Digital Signature](https://www.baeldung.com/java-digital-signature)

<a name="limitations"></a>
### Limitations <a href="#contents">^</a>

The system currently does not use multipart puts to S3 for writing processing data so output data files are limited to 5G in total compressed size.