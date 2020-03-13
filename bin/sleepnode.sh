#!/usr/bin/env bash

while [[ ! -f "${1}" ]]; do
    sleep 5
    echo "waiting for build…"
done
sleep 5
node "${1}"
