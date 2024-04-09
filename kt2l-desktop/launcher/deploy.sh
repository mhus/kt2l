#!/bin/bash

cd "$(dirname "$0")"
cd ../target

if [ ! -f launcher/KT2L.dmg ]; then
    echo "Fatal: KT2L.dmg not found"
    exit 1
fi

rm -rf deploy
mkdir deploy
cd deploy

NOW=$(date +"%Y-%m-%d")
git clone https://github.com/mhus/kt2l.git -b gh-pages gh-pages || exit 1
cd gh-pages

FILENAME=kt2l-desktop-mac-$NOW.dmg
TITLE="Desktop Mac OSX Bundled"
DESCRIPTION="Can be executed directly in Mac OS X M1. Java JDK 21 is included."

# cleanup
rm ./docs/snapshots/kt2l-desktop-mac-*
# copy
cp ../../launcher/KT2L.dmg ./docs/snapshots/$FILENAME || exit 1
# create download information
cat kt2l.org/templates/download.ts \
 | sed s/xfilenamex/$FILENAME/g \
 | sed s/xcreatedx/$NOW/g \
 | sed s/xtitlex/"$TITLE"/g \
 | sed s/xdescriptionx/"$DESCRIPTION"/g \
 > kt2l.org/src/downloads/download-snapshot-desktop-mac.ts  || exit 1

git add ./docs/snapshots/$FILENAME
git add ./kt2l.org/src/downloads/download-snapshot-desktop-mac.ts
git commit -m "Update desktop mac snapshot $NOW"
git push

