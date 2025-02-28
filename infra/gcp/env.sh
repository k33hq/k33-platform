#!/usr/bin/env bash

#
#  Switch environments
#

set -e

# checking bash version
if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then
  echo "This script requires Bash version >= 4"
  exit 1
fi

if [[ $# != 1 || "$1" != "dev" && "$1" != "prod" ]]; then
  echo "Usage: $0 (dev|prod)"
  exit 1
fi

rm -f .env infra/gcp/secrets/gcp-service-account.json
ENV=$1 op inject -i .env.template -o .env
op read op://k33-platform-env/$1/gcp/GCP_SA_KEY > infra/gcp/secrets/gcp-service-account.json

gcloud config configurations activate k33-$1
gcloud auth activate-service-account --key-file=infra/gcp/secrets/gcp-service-account.json