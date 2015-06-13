#!/bin/bash

set -x

on_error() {
  echo $1
  exit 1
}
DEPLOYER=../willow-deployer/target/deployer.sh
if ! [ -x $DEPLOYER ]; then
  on_error "Deployer script not found"
fi

if [ -z "$SERVER_PORT" ]; then
  SERVER_PORT=5120
fi

if [ -z "$SSH_AUTH_SOCK" ]; then
  TMP=$(mktemp)
  ssh-agent > $TMP
  source $TMP
  rm $TMP
  export SSH_AUTH_SOCK
  AGENT_STARTED="true"
fi
chmod 600 src/test/resources/id_rsa
ssh-add src/test/resources/id_rsa

JACOCO_PREFIX="-javaagent:target/jacoco-agent.jar=jmx=true,destfile=target/"
export W_JAVA_OPTS="-Dserver.port=$SERVER_PORT "$JACOCO_PREFIX"willow-deployer/run-its.exec"
bash -x $DEPLOYER start integration-test file:src/test/resources/integration-test.properties &
sleep 60
casperjs test --verbose --no-colors --concise --home=http://localhost:$SERVER_PORT src/test/casperjs/suites

export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/status.exec"
$DEPLOYER status integration-test
export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/jmxoperation.exec"
$DEPLOYER jmxoperation integration-test metrics_server "org.eclipse.jetty.util.thread:type=queuedthreadpool,id=0" threads
export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/stop.exec"
$DEPLOYER stop integration-test

if [ -n "$AGENT_STARTED" ]; then
  kill "$SSH_AGENT_PID"
fi
