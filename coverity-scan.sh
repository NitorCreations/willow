#!/bin/bash

cov-build --dir cov-int mvn -DskipTests=true clean package
cov-emit-java --dir cov-int --skip-war-sanity-check --war willow-servers/target/willow-servers-1.1-SNAPSHOT-uber.jar
tar czvf willow.tgz cov-int
curl --form token=PtcYbsmgkpf7VaBWR3a4gg   --form email=pasi.niemi@iki.fi   --form file=@willow.tgz   --form version="1.1-SNAPSHOT"   --form description="Prepare for release" https://scan.coverity.com/builds?project=NitorCreations%2Fwillow
rm -rf willow.tgz cov-int
