#!/bin/bash

if [ ! -d ../kt2l-core/src/main/resources/docs/index.html ]; then
  echo "Warn: docs not found in kt2l-core"
else
  cp -r ../kt2l-core/src/main/resources/docs ../kt2l-server/src/main/resources/
fi