#!/bin/bash

set -x

on_error() {
  echo $1
  exit 1
}
wait_for_data() {
  while ! curl -sf 'http://admin:admin@localhost:'$SERVER_PORT'/metrics/available?start='$START'&stop='$(($(date +%s) * 1000))'&tag=host_integrationtest' | grep '"/cpu":true'; do
    echo "Waiting for data"
    sleep 2
  done
}

DEPLOYER=../willow-deployer/target/deployer.sh
if ! [ -x $DEPLOYER ]; then
  on_error "Deployer script not found"
fi

if [ -z "$SERVER_PORT" ]; then
  SERVER_PORT=5120
fi

if [ -z "$DEPLOYER_PORT" ]; then
  DEPLOYER_PORT=5121
fi

if [ -z "$SSH_AUTH_SOCK" ]; then
  TMP=$(mktemp)
  ssh-agent > $TMP
  source $TMP
  rm $TMP
  export SSH_AUTH_SOCK
  AGENT_STARTED="true"
fi
if [ -z "$BUILD_NUMBER" ]; then
  export BUILD_NUMBER=1
fi
if [ -d ../.repository ]; then
  REPO="-Dmaven.repo.local=$(cd .. && pwd -P)/.repository"
fi
chmod 600 src/test/resources/id_rsa
ssh-add src/test/resources/id_rsa
rm ~/.sincedb_*

export WILLOW_VERSION=$(grep '<version>' pom.xml  | head -1 | awk -F'<|>' '{ print $3 }')

JACOCO_PREFIX="-javaagent:target/jacoco-agent.jar=jmx=true,destfile=target/"
export W_JAVA_OPTS="-Denduser.port=$SERVER_PORT -Ddeployer.port=$DEPLOYER_PORT "$JACOCO_PREFIX"willow-deployer/run-its.exec"
bash -x $DEPLOYER start integration-test file:src/test/resources/integration-test.properties &
START=$(($(date +%s) * 1000 - 15000))
wait_for_data
casperjs test --verbose --no-colors --concise --home=http://localhost:$SERVER_PORT src/test/casperjs/suites
TEST_RETURN=$?

java -Dwillow.port=$SERVER_PORT -cp target/test-classes/:target/willow-servers-$WILLOW_VERSION-uber.jar com.nitorcreations.willow.servers.test.ExampleEchoServer
TEST_RETURN=$(($TEST_RETURN + $?))

TEST_STR=$(date | md5sum | cut -d" " -f1)
export MAVEN_OPTS=$JACOCO_PREFIX"willow-deployer/maven1.exec"
mvn $REPO -B -e -f target/test-classes/deploy-pom.xml clean install willow:upload
TEST_RETURN=$(($TEST_RETURN + $?))
if ! [ -r target/metrics/deploydata/properties/1.0-SNAPSHOT.$BUILD_NUMBER/properties/root.properties ]; then
  echo "Test system properties not found"
  TEST_RETURN=$(($TEST_RETURN + 1))
fi
export BUILD_NUMBER=$(($BUILD_NUMBER + 1))
export MAVEN_OPTS=$JACOCO_PREFIX"willow-deployer/maven2.exec"
echo "integration1=$TEST_STR" >> target/test-classes/src/main/resources/root.properties
mvn $REPO -B -e -f target/test-classes/deploy-pom.xml clean install willow:upload
TEST_RETURN=$(($TEST_RETURN + $?))
if ! grep "integration1=$TEST_STR" target/metrics/deploydata/properties/1.0-SNAPSHOT.$BUILD_NUMBER/properties/root.properties > /dev/null; then
  echo "Test system updated properties not found"
  TEST_RETURN=$(($TEST_RETURN + 1))
fi
export MAVEN_OPTS=$JACOCO_PREFIX"willow-deployer/maven3.exec"
mvn $REPO -B -e -f target/test-classes/deploy-pom.xml willow:properties
TEST_RETURN=$(($TEST_RETURN + $?))
if ! grep "integration1=$TEST_STR" target/test-classes/target/application.properties > /dev/null; then
  echo "Properties merge failed"
  TEST_RETURN=$(($TEST_RETURN + 1))
fi

export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/status.exec"
$DEPLOYER status integration-test
TEST_RETURN=$(($TEST_RETURN + $?))

export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/jmxoperation.exec"
$DEPLOYER jmxoperation integration-test metrics_server "org.eclipse.jetty.util.thread:type=queuedthreadpool,id=0" threads
TEST_RETURN=$(($TEST_RETURN + $?))

export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/restartchild.exec"
$DEPLOYER restartchild integration-test
TEST_RETURN=$(($TEST_RETURN + $?))

sleep 30
wait_for_data
export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/status2.exec"
OUTPUT=$($DEPLOYER status integration-test)
echo "Status: "$OUTPUT
echo $OUTPUT | grep 'restarts: 2'
TEST_RETURN=$(($TEST_RETURN + $?))

export W_JAVA_OPTS=$JACOCO_PREFIX"willow-deployer/stop.exec"
$DEPLOYER stop integration-test
TEST_RETURN=$(($TEST_RETURN + $?))

if [ -n "$AGENT_STARTED" ]; then
  kill "$SSH_AGENT_PID"
fi
exit $TEST_RETURN
