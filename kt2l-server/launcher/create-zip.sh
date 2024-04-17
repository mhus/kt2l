#!/bin/bash
#
# This file is part of kt2l-server.
#
# kt2l-server is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# kt2l-server is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with kt2l-server.  If not, see <http://www.gnu.org/licenses/>.
#


cd "$(dirname "$0")"
cd ../target
rm -rf kt2l-server
mkdir kt2l-server
cd kt2l-server
cp ../kt2l-server-0.0.1-SNAPSHOT.jar .
cp -r ../../launcher/package/* .
cp -r ../../../config .
cd ..
zip -r kt2l-server.zip kt2l-server
