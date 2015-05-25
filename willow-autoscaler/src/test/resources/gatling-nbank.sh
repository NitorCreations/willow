#!/bin/bash

mvn -Dlogback.configurationFile=src/test/resources/logback-test.xml -Pnbank-load gatling:execute
