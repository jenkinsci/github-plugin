#!/usr/bin/env bash

(
    while true
    do
        sleep 60 && echo "NO_SLEEP_TRAVIS"
    done
)&

mvn package | grep -v Download