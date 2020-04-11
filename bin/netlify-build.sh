#!/usr/bin/env bash

lein clean
npm install -g sass
mkdir -p dist/css
touch dist/css/main.css
sass src/scss/main.scss dist/css/main.css
lein cljsbuild once ui-main
lein cljsbuild once ui-admin
