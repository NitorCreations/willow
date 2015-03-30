#!/bin/bash

MD5=$(md5sum target/willow-deployer-*-jar-with-dependencies.jar | cut -d " " -f 1)
ARCHIVE_START=$(($(wc -l  src/main/shell/deployer.sh | cut -d" " -f1) + 1))
sed -e "s/@@MD5@@/$MD5/" -e "s/@@ARCHIVE_START@@/$ARCHIVE_START/" src/main/shell/deployer.sh > target/deployer.sh
cat target/willow-deployer-*-jar-with-dependencies.jar >> target/deployer.sh
chmod 755 target/deployer.sh

BATCH_LINES=$(wc -l  src/main/shell/deployer.cmd | cut -d" " -f1)
sed -e "s/@@MD5@@/$MD5/" -e "s/@@BATCH_LINES@@/$BATCH_LINES/" src/main/shell/deployer.cmd > target/deployer.cmd
unix2dos target/deployer.cmd
cat target/willow-deployer-*-jar-with-dependencies.jar >> target/deployer.cmd

ln target/willow-deployer-*-jar-with-dependencies.jar target/deployer-uber-$MD5.jar
