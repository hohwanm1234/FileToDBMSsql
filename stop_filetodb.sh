#!/bin/sh

pidfile="/EAI/xconadm/XConnector/FileToDBMssql/tmp/filetodbmssql.pid"

if [ -f "$pidfile" ]
then
  
  kill -9 $(cat $pidfile)
  
  rm -rf $pidfile
      
  echo "=============================================================="
  echo "FileToDbMssql(IF_KR_ESIEMENS_GERP_HRH0400) Stop is Completed !"
  echo "=============================================================="
else
  echo "FileToDbMssql PID File is not exist"
  echo "Run the FileToDbMssql Service first..."
fi
