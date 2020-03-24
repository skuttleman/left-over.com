#!/usr/bin/env bash

set -e

ADMIN_BUILDS="locations shows"
PUB_BUILDS="images videos shows"

function build() {
    BUILD_ID="${1}"
    BUILD_ZIP="${2}"
    BUILD_TARGET="${3}"

    echo "building ${BUILD_ID}"
    rm -rf node_modules
    rm -f package.json
    rm -f package-lock.json
    rm -rf target/js
    rm -rf target/admin
    rm -rf target/pub
    lein with-profile build cljsbuild once ${BUILD_ID}
    zip -r target/${BUILD_ZIP}.zip node_modules
    zip -j target/${BUILD_ZIP}.zip target/${BUILD_TARGET}.js
}

function deploy() {
    FUNCTION="${1}"
    BUILD_ZIP="${2}"

    ERROR=$(aws lambda update-function-code --region us-east-1 --function-name "left-over--${FUNCTION}" --zip-file "fileb://target/${BUILD_ZIP}.zip" 2>&1 >/dev/null)

    if [[ ! -z "$ERROR" ]]; then
        echo "${ERROR}"
        exit 1
    fi

    echo "${FUNCTION} deployed"
}

lein clean
rm -rf target
mkdir target

build "auth" "auth" "auth"
deploy "auth" "auth"

for BUILD in ${ADMIN_BUILDS}; do
    build "admin-${BUILD}" "admin.${BUILD}" "admin/${BUILD}"
    deploy "admin-${BUILD}" "admin.${BUILD}"
done

for BUILD in ${PUB_BUILDS}; do
    build "pub-${BUILD}" "pub.${BUILD}" "pub/${BUILD}"
    deploy "${BUILD}" "pub.${BUILD}"
done

echo "all lambdas deployed"
