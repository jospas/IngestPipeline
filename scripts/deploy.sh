#!/bin/bash

mvn clean

scripts/cloudformation/deploy_infrastructure.sh

scripts/cloudformation/deploy_roles.sh

scripts/cloudformation/deploy_input_lambda.sh

scripts/cloudformation/deploy_processed_lambda.sh


