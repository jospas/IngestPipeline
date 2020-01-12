#!/bin/bash

aws s3 rb --force s3://dev-input-mytestsystem-255429042063

aws s3 rb --force s3://dev-processed-mytestsystem-255429042063

aws cloudformation delete-stack --stack-name mytestsystem-input-lambda

aws cloudformation delete-stack --stack-name mytestsystem-processed-lambda

aws cloudformation wait stack-delete-complete --stack-name mytestsystem-input-lambda

aws cloudformation wait stack-delete-complete --stack-name mytestsystem-processed-lambda

aws cloudformation delete-stack --stack-name mytestsystem-roles

aws cloudformation wait stack-delete-complete --stack-name mytestsystem-roles

aws cloudformation delete-stack --stack-name mytestsystem-infrastructure

aws cloudformation wait stack-delete-complete --stack-name mytestsystem-infrastructure


