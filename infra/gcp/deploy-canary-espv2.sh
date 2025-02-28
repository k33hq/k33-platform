#!/usr/bin/env bash

#
#  Script to deploy esp v2 to GCP cloud run.
#

set -e

# checking bash version
if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then
  echo "This script requires Bash version >= 4"
  exit 1
fi

# init env vars from .env.gcp
#if [ -f .env.gcp ]; then
#  set -o allexport
#  source .env.gcp
#  set +o allexport
#fi

# loading secrets from 1password
ENV=prod

GCP_PROJECT_ID=$(op read op://k33-platform-env/$ENV/gcp/GCP_PROJECT_ID)
GCP_BACKEND_HOST=$(op read op://k33-platform-env/$ENV/gcp/GCP_BACKEND_HOST)
CORS_REGEX=$(op read op://k33-platform-env/$ENV/gcp/CORS_REGEX)


# Deploy endpoints service

## temp dir to store modified OpenAPI file
TMP_DIR=$(mktemp -d)
echo "$TMP_DIR"
TMP_FILE="$TMP_DIR/k33-backend-canary-api.yaml"
trap 'rm -rf "$TMP_DIR"' EXIT

sed 's~${GCP_PROJECT_ID}~'"${GCP_PROJECT_ID}"'~g; s~${GCP_BACKEND_HOST}~'"${GCP_BACKEND_HOST}"'~g' libs/clients/k33-backend-client/src/main/openapi/k33-backend-canary-api.yaml >"$TMP_FILE"

gcloud endpoints services deploy "$TMP_FILE"

# Build ESP docker

## define ESP docker name and tag

declare -A espCloudRun
espCloudRun["service"]="k33-backend-canary-gateway"
espCloudRun["endpoint_service"]="canary.api.k33.com"
espCloudRun["service_account"]="k33-backend-gateway"

echo "espCloudRun[service]: ${espCloudRun["service"]}"
echo "espCloudRun[endpoint_service]: ${espCloudRun["endpoint_service"]}"
echo "espCloudRun[service_account]: ${espCloudRun["service_account"]}"

# Default to the latest released ESPv2 version.
BASE_IMAGE_NAME="gcr.io/endpoints-release/endpoints-runtime-serverless"
ESP_TAG="2"

echo "Determining fully-qualified ESP version for tag: ${ESP_TAG}"

ALL_TAGS=$(gcloud container images list-tags "${BASE_IMAGE_NAME}" \
  --filter="tags~^${ESP_TAG}$" \
  --format="value(tags)")
IFS=',' read -ra TAGS_ARRAY <<<"${ALL_TAGS}"

if [ ${#TAGS_ARRAY[@]} -eq 0 ]; then
  error_exit "Did not find ESP version: ${ESP_TAG}"
fi

# Find the tag with the longest length.
ESP_FULL_VERSION=""
for tag in "${TAGS_ARRAY[@]}"; do
  if [ ${#tag} -gt ${#ESP_FULL_VERSION} ]; then
    ESP_FULL_VERSION=${tag}
  fi
done
echo "ESP_FULL_VERSION: ${ESP_FULL_VERSION}"

ENDPOINT_SERVICE="${espCloudRun["endpoint_service"]}"

echo "ENDPOINT_SERVICE: ${ENDPOINT_SERVICE}"

espCloudRun["service_config"]="$(gcloud endpoints configs list --service "$ENDPOINT_SERVICE" --format=object --flatten=id --sort-by=~id --limit=1)"
echo "service_config: ${espCloudRun["service_config"]}"

espCloudRun["image"]="europe-docker.pkg.dev/${GCP_PROJECT_ID}/backend/endpoints-runtime-serverless:${ESP_FULL_VERSION}-${espCloudRun["endpoint_service"]}-${espCloudRun["service_config"]}"

echo "espCloudRun[image]: ${espCloudRun["image"]}"

## build and push ESP docker image
./apps/esp-v2/gcloud_build_image.sh \
  -s "${espCloudRun["endpoint_service"]}" \
  -c "${espCloudRun["service_config"]}" \
  -p "${GCP_PROJECT_ID}" \
  -v "${ESP_FULL_VERSION}" \
  -g europe-docker.pkg.dev/"$GCP_PROJECT_ID"/backend \
  -r "${CORS_REGEX}"

# Deploy ESP to GCP Cloud Run
gcloud run deploy "${espCloudRun["service"]}" \
  --region europe-west1 \
  --image "${espCloudRun["image"]}" \
  --cpu=1 \
  --memory=512Mi \
  --min-instances=1 \
  --max-instances=1 \
  --concurrency=1000 \
  --service-account "${espCloudRun["service_account"]}" \
  --allow-unauthenticated \
  --port=8080 \
  --platform=managed
