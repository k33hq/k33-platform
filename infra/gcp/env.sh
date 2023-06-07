#!/usr/bin/env bash

#
#  Switch environments
#

set -e

if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then
  echo "This script requires Bash version >= 4"
  exit 1
fi

if [[ $# != 1 || "$1" != "dev" && "$1" != "prod" ]]; then
  echo "Usage: env.sh (dev|prod)"
  exit 1
fi

rm .env .env.gcp
ln -s .env.$1 .env
ln -s .env.gcp.$1 .env.gcp

gcloud config configurations activate k33-$1
gcloud auth activate-service-account --key-file=infra/gcp/secrets/gcp-service-account-$1.json