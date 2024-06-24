#!/bin/bash
#
# kt2l-server - kt2l as server
# Copyright © 2024 Mike Hummel (mh@mhus.de)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#

cd "$(dirname "$0")"
cd ..

PID_FILE=var/run/kt2l-server.pid
export KT2L_LOG_DIRECTORY=var/logs
RUN=./bin/run.sh

. ./env.sh

LOG_FILE=${LOG_FILE:-$KT2L_LOG_DIRECTORY/stdout.log}

function wait_for_pid {
    PID=$1
    while ps -p $1 > /dev/null; do
        sleep 1
    done
}

case $1 in
    zap)
        if [ -f $PID_FILE ]; then
            PID=$(cat $PID_FILE)
            kill -9 $PID
            rm $PID_FILE
            wait_for_pid $PID
            echo "kt2l-server stopped"
        else
            echo "kt2l-server is not running"
        fi
        ;;
    kill)
        for i in $(ps|grep java|grep kt2l-server|cut -d ' ' -f 1) ; do
          echo "killing $i"
          kill -9 $i ;
        done
        ;;
    start)
        if [ -f $PID_FILE ]; then
            PID=$(cat $PID_FILE)
            if ps -p $PID > /dev/null; then
                echo "kt2l-server is already running"
                exit 1
            fi
        fi
        # rotate log file
        if [ "x$KT2L_ROTATE_STDOUT" == "xtrue" ]; then
            for i in $(seq 8 -1 1); do
                if [ -f $LOG_FILE.$i ]; then
                    rm $LOG_FILE.$((i+1)) || true
                    mv $LOG_FILE.$i $LOG_FILE.$((i+1))
                fi
            done
            if [ -f $LOG_FILE ]; then
                rm $LOG_FILE.1 || true
                mv $LOG_FILE $LOG_FILE.1
            fi
        fi
        # start in background
        export KT2L_SPRING_PROFILES=logtofile
        nohup $RUN > $LOG_FILE 2>&1 &
        echo $! > $PID_FILE
        echo "kt2l-server started on PID $(cat $PID_FILE) and PORT ${SERVER_PORT:-9080}"
        ;;
    stop)
        if [ -f $PID_FILE ]; then
            PID=$(cat $PID_FILE)
            kill $PID
            wait_for_pid $PID
            rm $PID_FILE
            echo "kt2l-server stopped"
        else
            echo "kt2l-server is not running"
        fi
        ;;
    restart)
        $0 stop
        $0 start
        ;;
    status)
        if [ -f $PID_FILE ]; then
            PID=$(cat $PID_FILE)
            if ps -p $PID > /dev/null; then
                echo "kt2l-server is running on PID $PID"
            else
                echo "kt2l-server is not running"
            fi
        else
            echo "kt2l-server is not running"
        fi
        for i in $(ps|grep java|grep kt2l-server|cut -d ' ' -f 1) ; do
          echo "server running on $i"
        done
        ;;
    stdout)
        if [ -f $LOG_FILE ]; then
            less +F $LOG_FILE
        else
            echo "No stdout log available"
        fi
        ;;
    log)
        F=$(ls -v $KT2L_LOG_DIRECTORY/server_*|tail -1)
        if [ -f $F ]; then
            tail -f $F
        else
            echo "No logs available"
        fi
        ;;
    lesslog)
        F=$(ls -v $KT2L_LOG_DIRECTORY/server_*|tail -1)
        if [ -f $F ]; then
            less +F $F
        else
            echo "No logs available"
        fi
        ;;
    *)
        echo "Usage: $0 {start|stop|status|zap|kill|logs}"
        exit 1
        ;;
esac