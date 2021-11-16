#!/usr/local/bin/bash

#
#  Script to deploy endpoints service.
#

#### checking bash version
if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then
  echo "This script requires Bash version >= 4"
  exit 1
fi

#### init env vars from .env
if [ -f .env ]; then
  set -o allexport
  source .env
  set +o allexport
fi

# Deploy endpoints service

## temp dir to store modified OpenAPI file
TMP_DIR=$(mktemp -d)
echo "$TMP_DIR"
TMP_FILE="$TMP_DIR/arcane-platform-test-api.yaml"
trap 'rm -rf "$TMP_DIR"' EXIT

#### since GCP_BACKEND_URL has '/', using '~' as delimiter for sed.
sed 's~${GCP_PROJECT_ID}~'"${GCP_PROJECT_ID}"'~g' libs/clients/arcane-platform-client/src/main/openapi/arcane-platform-test-api.yaml >"$TMP_FILE"

gcloud endpoints services deploy "$TMP_FILE"
