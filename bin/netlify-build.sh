#!/usr/bin/env bash

lein clean
npm install -g sass
sass src/scss/main.scss dist/css/main.css
lein cljsbuild once min
for ENV_VAR in API_HOST; do
    echo "window.${ENV_VAR} = '$(printenv $ENV_VAR)';" >> dist/js/env.js
done
