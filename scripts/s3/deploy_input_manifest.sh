#!/bin/bash

aws s3 cp --sse aws:kms \
    --sse-kms-key-id 9f2f32e1-d8e4-47ba-a928-04613b82e67c \
    data/customers.csv \
    s3://dev-input-mytestsystem-255429042063/input/

aws s3 cp --sse aws:kms \
    --sse-kms-key-id 9f2f32e1-d8e4-47ba-a928-04613b82e67c \
    data/manifest.json \
    s3://dev-input-mytestsystem-255429042063/input/
