#!/bin/sh

pidfile="/EAI/xconadm/XConnector/FileToDBMssql/tmp/filetodbmssql.pid"

if [ -f "$pidfile" ]
then
  PID=`cat $pidfile`

  if ps -p $PID > /dev/null
  then
      echo "\ FileToDBMssql Server [$PID] is now Running\nRun the Shutdown Shell first\n"
      exit 0
  fi
fi


## view /EAI/xconadm/XConnector/FileToDBMssql/src/bak/java_mkl > /EAI/xconadm/XConnector/FileToDBMssql/src/bak/java_mkl_log 2>&1 &
java -jar fileToDbMssql-1.0.0-jar-with-dependencies.jar > /EAI/xconadm/XConnector/FileToDBMssql/logs/access_sh.log 2>&1 &

echo $! > $pidfile

echo "================================================================"
echo "fileToDbMssql(IF_KR_ESIEMENS_GERP_HRH0400) Started Successfully!"
echo "log file name : start_filetodb.out"
echo "================================================================"
