[![Stories in Ready](https://badge.waffle.io/NitorCreations/willow.png?label=ready&title=Ready)](https://waffle.io/NitorCreations/willow)
[![Code Advisor On Demand Status](https://badges.ondemand.coverity.com/streams/belo7ihuvh5g17did9h651ihls)](https://ondemand.coverity.com/streams/belo7ihuvh5g17did9h651ihls/jobs)

# willow #


## Prerequisites ##

Build requires at least Java 7 and JCE (comes in OpenJDK by default). Also phantomjs is required for javascript unit testing.

## Starting statistics ui in development mode ##
Development mode means static resources are read in from
```
willow-servers/src/main/resources
```
This enables the editing of markup, css and javascript so that changes are immediately available in the ui.

First you need to build the server.
```
$ git clone git@github.com:NitorCreations/willow.git
$ cd willow
$ mvn clean install
```
Please note that building requires unlimited JCE to be installed (openjdk has this out of the  box, oracle jdk needs you to download a set of jars and extract them http://lmgtfy.com/?q=install+jce)

Deployer agent authenticetes with ssh agent signatures by default so you need to add a public key that matches a private key loaded into your ssh agent into willow-servers/src/main/resources/authorized_keys in openssh authorized_keys format.
```
$ willow-deployer/target/deployer.sh start test file:src/test/resources/develop-servers.properties
```
That starts the statistics server and starts feeding statistics from the local computer.
```
$ willow-deployer/target/deployer.sh stop test
```
That (in another terminal) stop everything. Alternatively
```
Ctrl-c Ctrl-z
$ kill -9 %1
```
In the same terminal kills the server

For windows there is willow-deployer/target/deployer.cmd that should work exactly the same way.
