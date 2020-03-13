#!/usr/bin/env bash

set -e

CLJS_BUILDS="images videos shows"

function build() {
    echo "building pub:${1}"
    rm -rf node_modules
    lein cljsbuild once pub-${1}
    zip -r target/pub.${1}.zip node_modules
    zip -j target/pub.${1}.zip target/pub/${1}.js
}

lein clean
rm -rf target
mkdir target

for CLJS_BUILD in ${CLJS_BUILDS}; do
    build ${CLJS_BUILD}
done

