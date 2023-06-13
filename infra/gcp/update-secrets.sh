#!/usr/bin/env bash

#
#  Script to update secrets for k33-backend to GCP cloud run.
#

set -e

# checking bash version
if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then
  echo "This script requires Bash version >= 4"
  exit 1
fi

# loading secrets from .env.gcp
#if [ -f .env.gcp ]; then
#  set -o allexport
#  source .env.gcp
#  set +o allexport
#fi


# loading secrets from 1password
if [[ $# != 1 || "$1" != "dev" && "$1" != "prod" ]]; then
  echo "Usage: $0 (dev|prod)"
  exit 1
fi

ENV=$1

SENDGRID_API_KEY=$(op read op://env/$ENV/sendgrid/SENDGRID_API_KEY)
LEGAL_SPACE_ID=$(op read op://env/$ENV/contentful/LEGAL_SPACE_ID)
LEGAL_SPACE_TOKEN=$(op read op://env/$ENV/contentful/LEGAL_SPACE_TOKEN)
RESEARCH_SPACE_ID=$(op read op://env/$ENV/contentful/RESEARCH_SPACE_ID)
RESEARCH_SPACE_TOKEN=$(op read op://env/$ENV/contentful/RESEARCH_SPACE_TOKEN)
ALGOLIA_APP_ID=$(op read op://env/$ENV/algolia/ALGOLIA_APP_ID)
ALGOLIA_API_KEY=$(op read op://env/$ENV/algolia/ALGOLIA_API_KEY)
SLACK_TOKEN=$(op read op://env/$ENV/slack/SLACK_TOKEN)
STRIPE_API_KEY=$(op read op://env/$ENV/stripe/STRIPE_API_KEY)
STRIPE_WEBHOOK_ENDPOINT_SECRET=$(op read op://env/$ENV/stripe/STRIPE_WEBHOOK_ENDPOINT_SECRET)
GOOGLE_ANALYTICS_API_SECRET=$(op read op://env/$ENV/analytics/GOOGLE_ANALYTICS_API_SECRET)



declare -A gcp_secrets

gcp_secrets[0]="SENDGRID_API_KEY"
gcp_secrets[1]="LEGAL_SPACE_ID"
gcp_secrets[2]="LEGAL_SPACE_TOKEN"
gcp_secrets[3]="RESEARCH_SPACE_ID"
gcp_secrets[4]="RESEARCH_SPACE_TOKEN"
gcp_secrets[5]="ALGOLIA_APP_ID"
gcp_secrets[6]="ALGOLIA_API_KEY"
gcp_secrets[7]="SLACK_TOKEN"
gcp_secrets[8]="STRIPE_API_KEY"
gcp_secrets[9]="STRIPE_WEBHOOK_ENDPOINT_SECRET"
gcp_secrets[10]="GOOGLE_ANALYTICS_API_SECRET"

# debugging
#for index in ${!gcp_secrets[@]}; do
#  echo ${gcp_secrets[$index]} = ${!gcp_secrets[$index]}
#done

index=0

while [ -n "${gcp_secrets["$index"]}" ]; do

  echo Updating secret: "${gcp_secrets["$index"]}"

  printf ${!gcp_secrets["$index"]} | gcloud secrets versions add "${gcp_secrets["$index"]}" \
    --data-file=-

  index=$((index + 1))

done