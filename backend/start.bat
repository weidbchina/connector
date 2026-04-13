@echo off
if not exist "logs" mkdir "logs"

set "JAVA_OPTS=-Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/heapdump.hprof"
set "APP_JAR=target\db-monitor-1.0.0-SNAPSHOT.jar"
set "LOG_FILE=logs\startup.log"

echo Starting DbMonitor...
echo Logs will be written to %LOG_FILE%

:: Start in background using javaw. 
:: We use cmd /c to allow redirection of stdout/stderr to file, and start /min to keep it out of way but persistent.
:: 'javaw' does not write to stdout usually. We use 'java' but detach it.
:: Actually, 'start /b' keeps it attached to console. 
:: The best approximation of 'nohup' on Windows without external tools is 'start /min java ...' (minimized window)
:: OR 'javaw' (no window, no console output).

:: Let's use javaw. To get logs, we really should configure Spring Boot to write to file, 
:: but for now, we will use 'java' inside a hidden window or minimized window to capture output.
:: Approach: start a minimized cmd that runs java and redirects output.

start "DbMonitor" /MIN cmd /c "java %JAVA_OPTS% -jar %APP_JAR% --server.port=8080 > %LOG_FILE% 2>&1"

echo Process started.
