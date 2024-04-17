#!/bin/bash
#
# This file is part of kt2l.
#
# kt2l is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# kt2l is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with kt2l.  If not, see <http://www.gnu.org/licenses/>.
#

cd $(dirname $0)/..

mvn de.mhus.mvn.plugin:tmpl-maven-plugin:2.0.0:tmpl || exit 1
mvn install -P assembly $@ || exit 1

./launch/bin/install.sh

