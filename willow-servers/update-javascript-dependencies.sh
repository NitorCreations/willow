#!/bin/bash

jspm update

for lib in $(xpath -q -e "//group[@name='libs']/*/text()" src/main/resources/wro.xml); do
  src=$(find target/jspm_packages/ -name $(basename $lib) -a -type f | head -1)
  if [ -n "$src" ]; then
    cp $src src/main/resources/webapp$lib
  fi
done
