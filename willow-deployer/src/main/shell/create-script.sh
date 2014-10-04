#!/bin/bash

MD5=$(md5sum target/willow-deployer-*-jar-with-dependencies.jar | cut -d " " -f 1)
sed "s/%%MD5%%/$MD5/" src/main/shell/deployer.sh > target/deployer.sh
cat target/willow-deployer-*-jar-with-dependencies.jar >> target/deployer.sh
chmod 755 target/deployer.sh
