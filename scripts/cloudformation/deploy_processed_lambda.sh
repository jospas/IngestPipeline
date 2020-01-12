#!/bin/bash

echo "Deploying processed lambda"

mvn clean package

aws cloudformation package \
    --template-file cloudformation/ProcessedLambda.yaml \
    --output-template-file cloudformation/ProcessedLambda-prepared.yaml \
    --profile aws-josh \
    --s3-bucket aws-joshpas-home \
    --s3-prefix applications/processedlambda/ \
    --region ap-southeast-2 \
    --force-upload

aws cloudformation deploy \
    --template-file cloudformation/ProcessedLambda-prepared.yaml \
    --profile aws-josh \
    --region ap-southeast-2 \
    --stack-name mytestsystem-processed-lambda \
    --parameter-overrides StageName=dev \
        SourceSystemName=mytestsystem
