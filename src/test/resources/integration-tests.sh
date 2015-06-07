#!/bin/bash

on_error() {
  echo $1
  exit 1
}

if ! [ -x willow-deployer/target/deployer.sh ]; then
  on_error "Deployer script not found"
fi

if [ -z "$SERVER_PORT" ]; then
  SERVER_PORT=5120
fi

JACOCO_PREFIX="-javaagent:target/jacoco-agent.jar=jmx=true,destfile=target/"
export W_JAVA_OPTS="-Dserver.port=$SERVER_PORT "$JACOCO_PREFIX"willow-deployer/run-its.exec"
bash -x willow-deployer/target/deployer.sh start integration-test file:src/test/resources/integration-test.properties &
sleep 60
casperjs test --verbose --no-colors --concise --home=http://localhost:$SERVER_PORT src/test/casperjs

export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/status.exec"
willow-deployer/target/deployer.sh status integration-test
export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/jmxoperation.exec"
willow-deployer/target/deployer.sh jmxoperation integration-test metrics_server "org.eclipse.jetty.util.thread:type=queuedthreadpool,id=0" threads
export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/stop.exec"
willow-deployer/target/deployer.sh stop integration-test
