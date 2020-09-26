#!/usr/bin/env bash

set -e

mkdir -p dist/css
lein sass auto
