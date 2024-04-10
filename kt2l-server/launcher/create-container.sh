#!/bin/bash

cd "$(dirname "$0")"
cd ..

if [ ! -f target/kt2l-server.zip ]; then
    echo "Fatal: kt2l-server.zip not found"
    exit 1
fi

docker build --progress=plain --platform linux/amd64 -t mhus/kt2l-server:snapshot -f launcher/Dockerfile .
