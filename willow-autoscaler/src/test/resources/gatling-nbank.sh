#!/bin/bash

mvn -Dlogback.configurationFile=src/test/resources/logback-gatling.xml -Pnbank-load gatling:execute
