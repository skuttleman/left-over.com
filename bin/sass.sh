#!/usr/bin/env bash

set -e

mkdir -p dist/css
touch dist/css/main.css
touch dist/css/main.css.map
lein sass auto
