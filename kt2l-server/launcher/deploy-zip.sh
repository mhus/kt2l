#!/bin/bash
#set -x
cd "$(dirname "$0")"
cd ../target

if [ ! -f kt2l-server.zip ]; then
    echo "Fatal: kt2l-server.zip not found"
    exit 1
fi

if [ -z "$AWS_ACCESS_KEY_ID" ]; then
    echo "Fatal: AWS_ACCESS_KEY_ID not correct set"
    exit 1
fi
if [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    echo "Fatal: AWS_SECRET_ACCESS_KEY not correct set"
    exit 1
fi

rm -rf deploy
mkdir deploy
cd deploy


NOW=$(date +"%Y-%m-%d")
git clone https://github.com/mhus/kt2l.git -b gh-pages gh-pages || exit 1
cd gh-pages

if [ "$(grep -c "extraheader" ../../../../.git/config)" -gt "0" ]; then
  set -x
  cd gh-pages
  pwd
  echo "[gc]" >> .git/config
  echo "	auto = 0" >> .git/config
  echo "[http \"https://github.com/\"]" >> .git/config
  cat ../../../../.git/config|grep "extraheader" >> .git/config
fi


FILENAME=kt2l-server-$NOW.zip
TITLE="Server Bundled"
DESCRIPTION="Can be started as Server and accessed via Browser, locally also. Java JDK 21 is required."
HREF="https://kt2l-downloads.s3.eu-central-1.amazonaws.com/snapshots/$FILENAME"

# cleanup
echo "Cleanup old snapshots in aws"
ENTRIES=$(aws s3 ls kt2l-downloads/snapshots/|sed "s/  / /g"|cut -d ' ' -f 4|grep -e ^kt2l-server)
echo Found $ENTRIES
for e in "$ENTRIES"; do
    echo "Delete $e"
    aws s3 rm "s3://kt2l-downloads/snapshots/$e"
done

# copy
echo "Copy kt2l-server.zip to aws"
aws s3 cp ../../kt2l-server.zip s3://kt2l-downloads/snapshots/$FILENAME || exit 1

# create download information
CREATED=$NOW
. ./kt2l.org/templates/download.ts.sh > kt2l.org/src/downloads/download-snapshot-server.ts

git add kt2l.org/src/downloads/download-snapshot-server.ts
git config --global user.name 'Robot'
git config --global user.email 'mhus@users.noreply.github.com'
git commit -m "Update server snapshot $NOW"
git push

