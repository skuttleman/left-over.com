#!/usr/bin/env bash

set -e

PUB_BUILDS="images videos shows"

function build() {
    BUILD_ID="${1}"
    BUILD_ZIP="${2}"
    BUILD_TARGET="${3}"

    echo "building ${BUILD_ID}"
    rm -rf node_modules
    rm -rf target/js
    rm -rf target/pub
    lein cljsbuild once ${BUILD_ID}
    zip -r target/${BUILD_ZIP}.zip node_modules
    zip -j target/${BUILD_ZIP}.zip target/${BUILD_TARGET}.js
}

lein clean
rm -rf target
mkdir target

build "auth" "auth" "auth"
for CLJS_BUILD in ${PUB_BUILDS}; do
    build "pub-${CLJS_BUILD}" "pub.${CLJS_BUILD}" "pub/${CLJS_BUILD}"
done
