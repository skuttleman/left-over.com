#!/usr/bin/env bash

lein clean
npm install -g sass
sass src/scss/main.scss dist/css/main.css
lein with-profile build cljsbuild once ui-min
