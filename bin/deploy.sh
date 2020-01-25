#!/bin/bash

set -e

lein do clean, uberjar
heroku deploy:jar target/website-0.1.0-SNAPSHOT-standalone.jar --app left-over-api
heroku run --app left-over-api -- java -cp $(ls target/*-standalone.jar) com.left_over.api.services.db.migrations migrate
