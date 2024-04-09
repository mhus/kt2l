#!/bin/bash

cd "$(dirname "$0")"
cd ../target

if [ ! -f kt2l-server.zip ]; then
    echo "Fatal: kt2l-server.zip not found"
    exit 1
fi

rm -rf deploy
mkdir deploy
cd deploy

NOW=$(date +"%Y-%m-%d")
git clone https://github.com/mhus/kt2l.git -b gh-pages gh-pages || exit 1
cd gh-pages

# cleanup
rm ./docs/snapshots/kt2l-server-*
# copy
cp ../../kt2l-server.zip ./docs/snapshots/kt2l-server-$NOW.zip || exit 1
# create download information
FILENAME=kt2l-server-$NOW.zip
TITLE="Server Bundled"
DESCRIPTION="Can be started as Server and accessed via Browser, locally also. Java JDK 21 is required."
cat kt2l.org/templates/download.ts \
 | sed s/xfilenamex/$FILENAME/g \
 | sed s/xcreatedx/$NOW/g \
 | sed s/xtitlex/"$TITLE"/g \
 | sed s/xdescriptionx/"$DESCRIPTION"/g \
 > kt2l.org/src/downloads/download-snapshot-server.ts  || exit 1

git add ./docs/snapshots/kt2l-server-$NOW.zip
git add ./kt2l.org/src/downloads/download-snapshot-server.ts
git commit -m "Update server snapshot $NOW"
git push

