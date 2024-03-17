#!/bin/bash
#
# Copyright (C) 2020 Mike Hummel (mh@mhus.de)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# config

REPO_PATH_ZIP="de/mhus/kt2l/±project_version±/kt2l-±project_version±-install.zip"
LOCAL_REPO_PATH_ZIP="$HOME/.m2/repository/$REPO_PATH_ZIP"
REMOTE_REPO_PATH_ZIP="https://repo1.maven.org/maven2/$REPO_PATH_ZIP"

# init

if [ ! -d $HOME/.kt2l/bin/±project_version± ]; then
  mkdir -p $HOME/.kt2l/bin/±project_version±
fi
if [ ! -d $HOME/.kt2l/tmp ]; then
  mkdir -p $HOME/.kt2l/tmp
fi

# download

if [ ! -e $LOCAL_REPO_PATH_ZIP ]; then
  if command -v mvn &> /dev/null; then
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:get \
      -Dartifact=de.mhus:kt2l:±project_version±:zip:install

  elif command -v curl &> /dev/null; then
    if [ -e $HOME/.kt2l/tmp/kt2l-install.zip ]; then
      rm $HOME/.kt2l/tmp/kt2l-install.zip
    fi
    curl --output $HOME/.kt2l/tmp/kt2l-install.zip $REMOTE_REPO_PATH_ZIP
    LOCAL_REPO_PATH_ZIP=$HOME/.kt2l/tmp/kt2l-install.zip
  else
     echo "Either mvn nor curl found - exit"
     exit 1
  fi
fi

if [ ! -e $LOCAL_REPO_PATH_ZIP ]; then
  echo "Can't download kt2l install zip"
  echo $REMOTE_REPO_PATH_ZIP
  exit 1
fi

# unpack and setup

cd $HOME/.kt2l/bin/±project_version±
unzip -o $LOCAL_REPO_PATH_ZIP
chmod +x $HOME/.kt2l/bin/±project_version±/*.sh

if [ -e $HOME/.kt2l/bin/kt2l ]; then
  rm $HOME/.kt2l/bin/kt2l
fi
ln -s $HOME/.kt2l/bin/±project_version±/kt2l.sh $HOME/.kt2l/bin/kt2l

# cleanup

if [ -e $HOME/.kt2l/tmp/kt2l-install.zip ]; then
  rm $HOME/.kt2l/tmp/kt2l-install.zip
fi

echo "Installed ±project_version± in $HOME/.kt2l"
echo "Add directory $HOME/.kt2l/bin to \$PATH (export PATH=$PATH:$HOME/.kt2l/bin/kt2l) or link $HOME/.kt2l/bin/kt2l in a binary directory like /usr/local/bin"
