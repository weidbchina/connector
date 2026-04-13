#!/bin/bash

# ==========================================
# 配置区域
# ==========================================

# 项目 JAR 包路径
APP_JAR="target/db-monitor-1.0.0-SNAPSHOT.jar"

# 启动端口
SERVER_PORT=8080

# JVM 参数配置
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./logs/heapdump.hprof"

# 日志文件路径
mkdir -p logs
LOG_FILE="logs/startup.log"

# ==========================================
# 启动逻辑
# ==========================================

echo "[INFO] Starting DbMonitor..."
echo "[INFO] JAR: $APP_JAR"
echo "[INFO] Port: $SERVER_PORT"
echo "[INFO] Logs: $LOG_FILE"

# 检查是否已经在运行
PID=$(ps -ef | grep "$APP_JAR" | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "[WARN] Application is already running (PID: $PID). Please stop it first."
    exit 1
fi

# nohup 后台启动
nohup java $JAVA_OPTS -jar $APP_JAR --server.port=$SERVER_PORT > $LOG_FILE 2>&1 &

NEW_PID=$!
echo "[INFO] Application started with PID: $NEW_PID"
echo "[INFO] You can tail the log with: tail -f $LOG_FILE"
