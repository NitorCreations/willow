#!/bin/bash

set -x
ROOT=$(cd $(dirname $0)/../../..; pwd -P)
mv $ROOT/target/willow-deployer/$1.exec $ROOT/target/willow-deployer/$1-$(date +%s).exec
