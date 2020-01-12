#!/bin/bash

echo "Deploying input lambda"

mvn clean package

aws cloudformation package \
    --template-file cloudformation/InputLambda.yaml \
    --output-template-file cloudformation/InputLambda-prepared.yaml \
    --profile aws-josh \
    --s3-bucket aws-joshpas-home \
    --s3-prefix applications/inputlambda/ \
    --region ap-southeast-2 \
    --force-upload

privateKey=`cat data/processed_private_key.txt`
publicKey=`cat data/input_public_key.txt`

aws cloudformation deploy \
    --template-file cloudformation/InputLambda-prepared.yaml \
    --profile aws-josh \
    --region ap-southeast-2 \
    --stack-name mytestsystem-input-lambda \
    --parameter-overrides StageName=dev \
        SourceSystemName=mytestsystem \
        PublicKey="$publicKey" \
        PrivateKey="$privateKey"
