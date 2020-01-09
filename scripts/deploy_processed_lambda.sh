#!/bin/bash

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
    --stack-name MyTestSystemProcessedLambdaStack \
    --parameter-overrides StageName=dev \
        SourceSystemName=mytestsystem
