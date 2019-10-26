#!/bin/bash

set -e

lein do clean, sass once, cljsbuild once min, uberjar
heroku deploy:jar target/website-0.1.0-SNAPSHOT-standalone.jar --app left-over-api
netlify deploy --dir=dist --prod
