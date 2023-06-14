#!/usr/bin/env bash

#
#  Updated github secrets
#

set -e

# checking bash version
if [ -z "${BASH_VERSINFO}" ] || [ -z "${BASH_VERSINFO[0]}" ] || [ ${BASH_VERSINFO[0]} -lt 4 ]; then
  echo "This script requires Bash version >= 4"
  exit 1
fi

op inject -i .env.github.template -o .env.github

gh secret set ENV_FILE \
  --repo k33hq/k33-platform \
  < .env.github

