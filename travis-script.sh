#!/usr/bin/env bash

(
    while true
    do
        sleep 60 && echo "NO_SLEEP_TRAVIS"
    done
)&

set -ex
set -o pipefail

mvn -U package | grep -v '^Download'
