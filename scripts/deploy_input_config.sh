#!/bin/bash

aws s3 cp --sse aws:kms \
    --sse-kms-key-id 3b3499f4-d737-4d3a-9d8f-e193a6644c7a \
    data/input_config.json \
    s3://dev-input-mytestsystem-255429042063/config/
