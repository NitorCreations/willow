#!/bin/bash

on_error() {
  echo $1
  exit 1
}

if ! [ -x willow-deployer/target/deployer.sh ]; then
  on_error "Deployer script not found"
fi

mvn -Prun-its dependency:copy
JACOCO_PREFIX="-javaagent:target/jacoco-agent.jar=jmx=true,destfile=target/"
export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/start.exec"
bash -x willow-deployer/target/deployer.sh start integration-test file:src/test/resources/integration-test.properties &
sleep 5
export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/status.exec"
willow-deployer/target/deployer.sh status integration-test
export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/stop.exec"
willow-deployer/target/deployer.sh stop integration-test
