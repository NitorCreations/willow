#!/bin/bash

NEW_MD5=%%MD5%%

if [ -r deployer.jar.md5 ]; then
  OLD_MD5=$(cat deployer.jar.md5 | cut -d " " -f 1)
fi

if [ "$NEW_MD5" != "$OLD_MD5" ]; then
  ARCHIVE=$(awk '/^__ARCHIVE_BELOW__/ {print NR + 1; exit 0; }' $0)
  tail -n+$ARCHIVE $0 > deployer.jar
  echo "$NEW_MD5 deployer.jar" > deployer.jar.md5
fi

if [ -z "$JAVA_HOME" ]; then
  if ! which java > /dev/null; then
    echo "No java or java on PATH"
    exit 1
  else
    JAVA_HOME=$(java -cp deployer.jar com.nitorcreations.willow.deployer.JavaHome)
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

exec $JAVA_HOME/bin/java -cp $JAVA_TOOLS:deployer.jar com.nitorcreations.willow.deployer.Main "$@"
__ARCHIVE_BELOW__
