#! /bin/sh

mkdir -p /data/logs
java -server \
     -Dsun.jnu.encoding=UTF-8 \
     -Dfile.encoding=UTF-8 \
     -Xloggc:/data/logs/gc.log \
     -XX:+PrintTenuringDistribution \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=oom.hprof \
     -XX:ErrorFile=error_sys.log \
     -Xms$BK_REPO_JVM_XMS \
     -Xmx$BK_REPO_JVM_XMX \
     -jar $MODULE.jar \
     --spring.profiles.active=$BK_REPO_ENV \
     --spring.cloud.consul.host=$HOST_IP

tail -f /dev/null