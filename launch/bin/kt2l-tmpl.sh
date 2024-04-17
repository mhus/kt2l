#!/bin/sh
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

# -----------------------------------------------------------------------------
# KT2L Startup Script
#
# Environment Variable Prerequisites
#
#   JAVA_HOME       Must point at your Java Development Kit installation.
#   KTOOL_OPTS      (Optional) Java runtime options used when kt2l is executed.
#   KTOOL_SKIP_RC   (Optional) Flag to disable loading of .kt2lrc files.
# -----------------------------------------------------------------------------

if [ -z "$KTOOL_SKIP_RC" ] ; then

  if [ -f /etc/kt2lrc ] ; then
    . /etc/kt2lrc
  fi

  if [ -f "$HOME/.kt2lrc" ] ; then
    . "$HOME/.kt2lrc"
  fi

fi

# OS specific support. $var _must_ be set to either true or false.
cygwin=false;
mingw=false;
case "`uname`" in
  CYGWIN*) cygwin=true;;
  MINGW*) mingw=true;;
esac

## resolve links - $0 may be a link to KT2L's home
PRG="$0"

# need this for relative symlinks
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG="`dirname "$PRG"`/$link"
  fi
done

saveddir=`pwd`

KTOOL_HOME=`dirname "$PRG"`/../..

# make it fully qualified
KTOOL_HOME=`cd "$KTOOL_HOME" && pwd`

cd "$saveddir"

# For Cygwin, ensure paths are in Unix format before anything is touched
if $cygwin ; then
  [ -n "$KTOOL_HOME" ] &&
    KTOOL_HOME=`cygpath --unix "$KTOOL_HOME"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# For MinGW, ensure paths are in Unix format before anything is touched
if $mingw ; then
  [ -n "$KTOOL_HOME" ] &&
    KTOOL_HOME=`(cd "$KTOOL_HOME"; pwd)`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`(cd "$JAVA_HOME"; pwd)`
  # TODO classpath?
fi

if [ -z "$JAVA_HOME" ] ; then
  JAVACMD=`which java`
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "The JAVA_HOME environment variable is not defined correctly" >&2
  echo "This environment variable is needed to run this program" >&2
  echo "NB: JAVA_HOME should point to a JDK not a JRE" >&2
  exit 1
fi

KTOOL_JAR=`echo "${KTOOL_HOME}"/bin/±project_version±/kt2l.jar`

# For Cygwin, switch paths to Windows format before running java
if $cygwin ; then
  [ -n "$KTOOL_HOME" ] &&
    KTOOL_HOME=`cygpath --path --windows "$KTOOL_HOME"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  [ -n "$CLASSWORLDS_JAR" ] &&
    CLASSWORLDS_JAR=`cygpath --path --windows "$CLASSWORLDS_JAR"`
fi

# traverses directory structure from process work directory to filesystem root
# first directory with .kt2l subdirectory is considered project base directory
find_ktool_basedir() {
(
  basedir=`find_file_argument_basedir "$@"`
  wdir="${basedir}"
  while [ "$wdir" != '/' ] ; do
    if [ -d "$wdir"/.kt2l ] ; then
      basedir=$wdir
      break
    fi
    wdir=`cd "$wdir/.."; pwd`
  done
  echo "${basedir}"
)
}

find_file_argument_basedir() {
(
  basedir=`pwd`

  found_file_switch=0
  for arg in "$@"; do
    if [ ${found_file_switch} -eq 1 ]; then
      if [ -d "${arg}" ]; then
        basedir=`cd "${arg}" && pwd -P`
      elif [ -f "${arg}" ]; then
        basedir=`dirname "${arg}"`
        basedir=`cd "${basedir}" && pwd -P`
        if [ ! -d "${basedir}" ]; then
          echo "Directory ${basedir} extracted from the -f/--file command-line argument ${arg} does not exist" >&2
          exit 1
        fi
      else
        echo "POM file ${arg} specified with the -f/--file command line argument does not exist" >&2
        exit 1
      fi
      break
    fi
    if [ "$arg" = "-f" -o "$arg" = "--file" ]; then
      found_file_switch=1
    fi
  done
  echo "${basedir}"
)
}

# concatenates all lines of a file
concat_lines() {
  if [ -f "$1" ]; then
    echo "`tr -s '\r\n' '  ' < "$1"`"
  fi
}

KTOOL_PROJECTBASEDIR="${KTOOL_BASEDIR:-`find_ktool_basedir "$@"`}"
KTOOL_OPTS="`concat_lines "$KTOOL_PROJECTBASEDIR/.kt2l/jvm.config"` $KTOOL_OPTS"

# For Cygwin, switch project base directory path to Windows format before
# executing KT2L otherwise this will cause KT2L not to consider it.
if $cygwin ; then
  [ -n "$KTOOL_PROJECTBASEDIR" ] &&
  KTOOL_PROJECTBASEDIR=`cygpath --path --windows "$KTOOL_PROJECTBASEDIR"`
fi

export KTOOL_PROJECTBASEDIR

# Provide a "standardized" way to retrieve the CLI args that will
# work with both Windows and non-Windows executions.
KTOOL_CMD_LINE_ARGS="$KTOOL_CONFIG $@"
export KTOOL_CMD_LINE_ARGS
export CP=$KTOOL_JAR

export JAVA_TOOL_OPTIONS=$KTOOL_JAVA_TOOL_OPTIONS
exec "$JAVACMD" \
  $KTOOL_OPTS \
  $KTOOL_DEBUG_OPTS \
  "-Dktool.home=${KTOOL_HOME}" \
  -jar ${CP} "$@"
