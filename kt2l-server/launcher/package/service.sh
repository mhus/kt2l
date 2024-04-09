#!/bin/bash

cd "$(dirname "$0")"

PID_FILE=kt2l-server.pid
LOG_FILE=kt2l-server.log
. ./env.sh

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

        nohup ./run.sh > $LOG_FILE 2>&1 &
        echo $! > $PID_FILE
        echo "kt2l-server started on $(cat $PID_FILE)"
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
    status)
        if [ -f $PID_FILE ]; then
            PID=$(cat $PID_FILE)
            if ps -p $PID > /dev/null; then
                echo "kt2l-server is running on $PID"
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
    logs)
        if [ -f $LOG_FILE ]; then
            tail -f $LOG_FILE
        else
            echo "No logs available"
        fi
        ;;
    *)
        echo "Usage: $0 {start|stop|status|zap|kill|logs}"
        exit 1
        ;;
esac