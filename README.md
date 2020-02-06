## Amazon Serverless Ingest Pipeline

<a name="contents"></a>
### Contents

- [Introduction](#introduction)
- [Getting started](#getting-started)
- [Installation](#installation)	
	- [Infrastructure template](#infrastructure)
	- [Lambda roles template](#lambda_roles)
	- [Input Lambda template](#input_lambda)
	- [Processed Lambda template](#processed_lambda)
	- [Copying configuration files](#copying)
	- [Inject sample customer data and manifest.json](#inject)
- [Building secure manifests](#building-manifests)

<a name="introduction"></a>
### Introduction

Proof of concept serverless ingest pipeline which aims to listen for input manifest files created in S3 and process a batch of CSV files using a Java based Lambda functions.

Hooks to add features to the processing pipeline to allow data enrichment and transformation are provided.

Deployment to an AWS account is achieved via AWS CloudFormation.

<a name="getting-started"></a>
### Getting Started

The system uses Apache Maven and is easily configured in IntelliJ by importing the Maven pom.xml as a new project.

### Building

Build either from within IntelliJ or use Maven:

```bash
mvn package
```
<a name="installation"></a>
### Installation

Deployment is achieved via AWS CloudFormation.

The templates should be deployed in the order detailed below.

<a name="infrastructure"></a>
#### Infrastructure Template

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

<a name="lambda_roles"></a>
#### Lambda Roles Template

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

<a name="input_lambda"></a>
#### Input Lambda Template

This template deploys the SQS and Lambda processing pipeline subscribed the input S3 bucket change notifications via SNS.

It requires the Lambda roles created in step 2).

A sample deployment script is provided:

	scripts/deploy_input_lambda.sh

To deploy by hand, prepare the stack for deployment via CloudFormation:

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
		SourceSystemName=<source system name> \
		PublicKey="<public key>" \
		PrivateKey="<private key>"
```

For example deploying a dev stack for MyTestSystem:

```bash

publicKey=`cat data/input_public_key.txt`
privateKey=`cat data/processed_private_key.txt`

aws cloudformation deploy \
	--template-file cloudformation/InputLambda-prepared.yaml \
	--profile aws-josh \
	--region ap-southeast-2 \
	--stack-name MyTestSystemInputLambdaStack \
	--parameter-overrides StageName=dev \
		SourceSystemName=mytestsystem \
		PublicKey="$publicKey" \
		PrivateKey="$privateKey" \
```

<a name="processed_lambda"></a>
#### Processed Lambda Template

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
		SourceSystemName=<source system name> \
		PublicKey="<public key>"
```

For example deploying a dev stack for MyTestSystem:

```bash

publicKey=`cat data/processed_public_key.txt`

aws cloudformation deploy \
	--template-file cloudformation/ProcessedLambda-prepared.yaml \
	--profile aws-josh \
	--region ap-southeast-2 \
	--stack-name MyTestSystemProcessedLambdaStack \
	--parameter-overrides StageName=dev \
		SourceSystemName=mytestsystem \
		PublicKey="$publicKey"
```
<a name="copying"></a>
#### 4) Copying configuration files

Edit the script:

	scripts/deploy_input_config.sh 

to use your bucket and KMS key ids and deploy some sample configuration by running this script.

<a name="inject"></a>
#### 5) Inject sample customer data and manifest.json

Edit the script:

	scripts/deploy_input_manifest.sh 
	
to use your bucket and KMS key ids and deploy a sample manifest.json for testing.


<a name="building-manifests"></a>

### Building secure Manifests

The system makes use of digital signatures to verify sender data integrity and identify. 

This requires the sender to produce manifest files with a hash signed by the source system's private key, can be verified by the ingest pipeline using the public key.

The following command line utility can be used to generate public and private keys:

	com.aws.ingest.security.KeyGenerator

The following command line utility class can be used to sign manifests:

	com.aws.ingest.security.SignManifest
	
The following command line utility class can be used to verify manifests:

	com.aws.ingest.security.VerifyManfest
	
See: [Java Digital Signatures](https://www.baeldung.com/java-digital-signature)


