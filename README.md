# willow #

## Starting statistics ui in development mode ##
Development mode means static resources are read in from
```willow-servers/src/main/resources```
This enables the editing of markup, css and javascript so that changes are immediately available in the ui
First you need to build the server.
```
$ git clone git@github.com:NitorCreations/willow.git
$ cd willow
$ mvn clean install
```
Please note that building requires unlimited JCE to be installed (openjdk has this out of the  box, oracle jdk needs you to download a set of jars and extract them http://lmgtfy.com/?q=install+jce)
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
