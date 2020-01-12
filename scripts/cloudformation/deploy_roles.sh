#!/bin/bash

echo "Deploying roles"

aws cloudformation deploy \
    --template-file cloudformation/LambdaRoles.yaml \
    --profile aws-josh \
    --region ap-southeast-2 \
    --stack-name mytestsystem-roles \
    --parameter-overrides StageName=dev \
        SourceSystemName=mytestsystem \
    --capabilities CAPABILITY_NAMED_IAM
