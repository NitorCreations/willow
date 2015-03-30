#!/bin/bash

MD5=%%MD5%%

if [ "$1" = "-d" ]; then
  shift
  if [[ "$1" =~ ^[0-9]*$ ]]; then
    DEBUG_PORT=$1
    shift
  else
    DEBUG_PORT=4444
  fi
  DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$DEBUG_PORT" 
fi

if [ -z "$W_DEPLOYER_HOME" ]; then
  W_DEPLOYER_HOME=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd -P)
  if [ "$W_DEPLOYER_HOME" = "/usr/bin" ]; then
    W_DEPLOYER_HOME="$HOME/.deployer"
    W_DEPLOYER_LIB="/var/lib/deployer"
  else
    W_DEPLOYER_LIB="$W_DEPLOYER_HOME/lib"
  fi
else
  W_DEPLOYER_LIB="$W_DEPLOYER_HOME/lib"
fi

if [ ! -d "$W_DEPLOYER_LIB" ]; then 
  mkdir -p "$W_DEPLOYER_LIB"
  if [ ! -d "$W_DEPLOYER_LIB" ]; then
    echo "Failed to create deployer lib directory"
    exit 1
  fi
fi

W_DEPLOYER_JAR=$W_DEPLOYER_LIB/deployer-uber-$MD5.jar
flock "$W_DEPLOYER_LIB" bash -c "if ! [ -r \"$W_DEPLOYER_JAR\" ]; then tail -n+%%ARCHIVE_START%% \"${BASH_SOURCE[0]}\" > \"$W_DEPLOYER_JAR\"; fi"

if [ -z "$JAVA_HOME" ]; then
  if ! which java > /dev/null; then
    echo "No java or java on PATH"
    exit 1
  else
    JAVA_HOME=$(java -cp $W_DEPLOYER_JAR com.nitorcreations.willow.deployer.JavaHome)
  fi
fi
if [ -d $JAVA_HOME/../lib ]; then
 JAVA_LIB=$(cd $JAVA_HOME/../lib; pwd)
elif [ -d $JAVA_HOME/lib ]; then
  JAVA_LIB=$(cd $JAVA_HOME/lib; pwd)
else
  echo "Could not find java lib dir"
  exit 2
fi

JAVA_TOOLS=$JAVA_LIB/tools.jar
W_DEPLOYER_NAME=$2
export W_DEPLOYER_NAME W_DEPLOYER_HOME W_DEPLOYER_JAR
case $1 in 
start)
  shift
  exec 2>&1
  exec $JAVA_HOME/bin/java $DEBUG -cp $JAVA_TOOLS:$W_DEPLOYER_JAR com.nitorcreations.willow.deployer.Main "$@"
  ;;
stop)
  shift
  exec $JAVA_HOME/bin/java $DEBUG -cp $JAVA_TOOLS:$W_DEPLOYER_JAR com.nitorcreations.willow.deployer.Stop "$@"
  ;;
status)
  shift
  exec $JAVA_HOME/bin/java $DEBUG -cp $JAVA_TOOLS:$W_DEPLOYER_JAR com.nitorcreations.willow.deployer.Status "$@"
  ;;
restartchild)
  shift
  exec $JAVA_HOME/bin/java $DEBUG -cp $JAVA_TOOLS:$W_DEPLOYER_JAR com.nitorcreations.willow.deployer.RestartChild "$@"
  ;;
jmxoperation)
  shift
  exec $JAVA_HOME/bin/java $DEBUG -cp $JAVA_TOOLS:$W_DEPLOYER_JAR com.nitorcreations.willow.deployer.JMXOperation "$@"
  ;;
*)
  echo "usage $0 {start|stop|status|jmxoperation} [role] url [url [...]]"
  exit 1
  ;;
esac

