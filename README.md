[![Stories in Ready](https://badge.waffle.io/NitorCreations/willow.png?label=ready&title=Ready)](https://waffle.io/NitorCreations/willow)
[![Stories in progress](https://badge.waffle.io/NitorCreations/willow.png?label=in%20progress&title=In%20progress)](https://waffle.io/NitorCreations/willow)
[![Stories ready to merge](https://badge.waffle.io/NitorCreations/willow.png?label=ready%20to%20merge&title=Ready%20to%20merge)](https://waffle.io/NitorCreations/willow)
[![Code Advisor On Demand Status](https://badges.ondemand.coverity.com/streams/jdq5h6193p18d9k86859ro7t0c)](https://ondemand.coverity.com/streams/jdq5h6193p18d9k86859ro7t0c/jobs)
# willow #


## Prerequisites ##

Build requires at least Java 7 and JCE (comes in OpenJDK by default). Also phantomjs is required for javascript unit testing.

## Starting statistics ui in development mode ##
Development mode means static resources are read in from `willow-servers/src/main/resources`.
This enables the editing of markup, css and javascript so that changes are immediately available in the ui.

1. Build the server

  ```
  $ git clone git@github.com:NitorCreations/willow.git
  $ cd willow
  $ mvn clean install
  ```
  Please note that building requires unlimited JCE to be installed (openjdk has this out of the  box, oracle jdk needs you to download a set of jars and extract them http://lmgtfy.com/?q=install+jce). **NOTE**: if JCE is not correctly installed, tests will fail with `java.lang.NoClassDefFoundError: Could not initialize class javax.crypto.JceSecurity`.

2. Ensure ssh identity is set

  **NOTE**: in the simplest development mode setup the statistics server and the deployer reside in the same machine, i.e. the statistics server monitors itself. 
  1. From deployer to statistics server: deployer agent authenticates with ssh agent signatures by default so you need to add a public key that matches a private key loaded into your ssh agent into
`willow-servers/src/main/resources/authorized_keys` in openssh authorized_keys format. 
     * To check the private keys loaded into your ssh agent, run `ssh-add -l`. 
     * To add a key, run `ssh-add ~/.ssh/id_rsa`.
  2. From statistics server to deployer node: ssh autologin need to be set up for the ui shell to work. To set up ssh autologin, run e.g. `cat ~/.ssh/id_rsa.pub | ssh b@B 'cat >> .ssh/authorized_keys'` 

3. Start the statistics server and feeding statistics from the local computer

  ```
  $ willow-deployer/target/deployer.sh start test file:src/test/resources/develop-servers.properties
  ```
  
  After this the ui should be accessible at `http://localhost:5120` ([users] (../master/willow-servers/src/main/resources/shiro.ini)).

4. To stop
  * in another terminal: `$ willow-deployer/target/deployer.sh stop test`
  * in same terminal:
  
    ```
    Ctrl-c Ctrl-z
    $ kill -9 %1
    ```

For **windows** there is `willow-deployer/target/deployer.cmd` that should work exactly the same way.
