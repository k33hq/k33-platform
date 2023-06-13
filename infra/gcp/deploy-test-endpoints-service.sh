#!/usr/bin/env bash

#
#  Script to deploy endpoints service.
#

set -e

# checking bash version
if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then
  echo "This script requires Bash version >= 4"
  exit 1
fi

# Deploy endpoints service
gcloud endpoints services deploy libs/clients/k33-backend-client/src/main/openapi/k33-backend-test-api.yaml
