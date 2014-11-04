#!/bin/bash

NEW_MD5=%%MD5%%

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

if [ -z "$DEPLOYER_HOME" ]; then
  DEPLOYER_HOME=$(cd $(dirname "${BASH_SOURCE[0]}") && pwd -P)
fi

if [ -r $DEPLOYER_HOME/deployer.jar.md5 ]; then
  OLD_MD5=$(cat $DEPLOYER_HOME/deployer.jar.md5 | cut -d " " -f 1)
fi

if [ "$NEW_MD5" != "$OLD_MD5" ]; then
  ARCHIVE=$(awk '/^__ARCHIVE_BELOW__/ {print NR + 1; exit 0; }' $0)
  tail -n+$ARCHIVE $0 > $DEPLOYER_HOME/deployer.jar
  echo "$NEW_MD5  deployer.jar" > $DEPLOYER_HOME/deployer.jar.md5
fi

if [ -z "$JAVA_HOME" ]; then
  if ! which java > /dev/null; then
    echo "No java or java on PATH"
    exit 1
  else
    JAVA_HOME=$(java -cp $DEPLOYER_HOME/deployer.jar com.nitorcreations.willow.deployer.JavaHome)
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
DEPLOYER_IDENTIFIER=$(< /dev/urandom tr -dc _A-Z-a-z-0-9 | head -c32; echo)
export W_DEPLOYER_NAME DEPLOYER_IDENTIFIER JAVA_TOOLS JAVA_HOME
case $1 in 
start)
  shift
  exec $JAVA_HOME/bin/java $DEBUG -cp $JAVA_TOOLS:$DEPLOYER_HOME/deployer.jar com.nitorcreations.willow.deployer.Main "$@"
  ;;
stop)
  shift
  exec $JAVA_HOME/bin/java $DEBUG -cp $JAVA_TOOLS:$DEPLOYER_HOME/deployer.jar com.nitorcreations.willow.deployer.Stop "$@"
  ;;
status)
  shift
  exec $JAVA_HOME/bin/java $DEBUG -cp $JAVA_TOOLS:$DEPLOYER_HOME/deployer.jar com.nitorcreations.willow.deployer.Status "$@"
  ;;
*)
  echo "usage $0 {start|stop|status} [role] url [url [...]]"
  exit 1
  ;;
esac


__ARCHIVE_BELOW__
