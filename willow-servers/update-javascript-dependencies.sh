#!/bin/bash

jspm update

for lib in $(xpath -q -e "//group[@name='libs']/*/text()" src/main/resources/wro.xml); do
  src=$(find target/jspm_packages/ -name $(basename $lib) -a -type f | head -1)
  if [ -n "$src" ]; then
    cp $src src/main/resources/webapp$lib
  fi
done
cp -a ./target/jspm_packages/github/components/jqueryui@*/themes/smoothness/images src/main/resources/webapp/styles/
cp ./target/jspm_packages/github/components/jqueryui@*/themes/smoothness/jquery-ui.css src/main/resources/webapp/styles/
