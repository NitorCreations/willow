# Building #

<pre>mvn clean install</pre> will do all required tests and packaging. Jasmine
tests requires <pre>phantomjs 1.9.x</pre> to be installed on the path.

# Managing Javascript dependencies #

This is the willow module with all the javascipt dependecies and they are
managed from here. A general rule is that every dependecy will cause it's share
of work for the lifetime of the project so, they should not be incuded frivoloysly.
I get the irony looking at the dependencies of the project now, but each
dependency has gotten is proper assessment so please be careful.

## Updating javascripts ##

To update the versions for current javascript dependencies, simply run
```
./update-javascript-dependencies.sh
```
It will run <pre>jspm update</pre> and loop through the dependencies found
in <pre>src/main/resources/wro.xml</pre> and copy each source file from
<pre>target/jspm_packages/</pre> to <pre>src/main/resources/webapp/[scripts|styles]</pre>
as approprieate. You wil need <pre>jspm</pre> and <pre>xpath</pre> installed for
this. After this check changes with <pre>git diff</pre> and check
changes in whereever needed. Currently there is a change to cubism rule
timeformats, so please keep that in manually.

## Adding a javascript dependency ##

Plaese keep the caveat above in mind before doing this. Firstly install the
package with jspm.
```
jspm install <package-name>=<package-source:github|npm|...>:<package-name>
```
Find the approprieate javascript and css files under <pre>target/jspm_packages</pre>
and add them to the <pre>lib</pre> group in <pre>src/main/resources/wro.xml</pre>.
Run javascript update as above. If your dependency can not be mocked out of the
jasmine tests, you will also need to add them to <pre>pom.xml</pre> jasmine
plugin configuration under <pre>//configuration/preloadSources/source</pre>
as with <pre>jquery</pre> et.al.
