#!/bin/bash

set -e

APP=${1:left-over-api}

lein do clean, uberjar
heroku deploy:jar target/website-0.1.0-SNAPSHOT-standalone.jar --app ${APP}
heroku run --app ${APP} -- java -cp $(ls target/*-standalone.jar) com.left_over.api.services.db.migrations migrate
