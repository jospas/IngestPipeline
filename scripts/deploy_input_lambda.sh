#!/bin/bash

mvn clean package

aws cloudformation package \
    --template-file cloudformation/InputLambda.yaml \
    --output-template-file cloudformation/InputLambda-prepared.yaml \
    --profile aws-josh \
    --s3-bucket aws-joshpas-home \
    --s3-prefix applications/inputlambda/ \
    --region ap-southeast-2 \
    --force-upload

aws cloudformation deploy \
    --template-file cloudformation/InputLambda-prepared.yaml \
    --profile aws-josh \
    --region ap-southeast-2 \
    --stack-name MyTestSystemInputLambdaStack \
    --parameter-overrides StageName=dev \
        SourceSystemName=mytestsystem
