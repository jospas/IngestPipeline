#!/bin/bash

echo "Deploying infrastructure"

aws cloudformation deploy \
    --template-file cloudformation/Infrastructure.yaml \
    --profile aws-josh \
    --region ap-southeast-2 \
    --stack-name mytestsystem-infrastructure \
    --parameter-overrides StageName=dev \
        SourceSystemName=mytestsystem
