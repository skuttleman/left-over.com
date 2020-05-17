#!/usr/bin/env bash

set -e

lein clean
mkdir -p dist/css
touch dist/css/main.css
sass src/scss/main.scss dist/css/main.css
bin/with-profile.sh test cljsbuild once ui-main
bin/with-profile.sh test cljsbuild once ui-admin
bin/with-profile.sh test doo $@
