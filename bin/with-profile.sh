#!/usr/bin/env bash

set -e

PROFILE="${1}"
shift
ENV_FILE=.lein-env.${PROFILE} lein with-profile ${PROFILE} $@
