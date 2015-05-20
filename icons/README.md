# Managing icons #

Add your icon to <pre>originals</pre> directory. You need to have the tools
installed so run:
```
npm i
```
After that run
```
grunt
```
This will create an svg containing all of the svgs in <pre>originals</pre> in
<pre>icons.svg</pre>. Then you need to add style="height: 0" attribute to the root
svg element so that the sprite is hidden. Finally that just needs to be copied to
<pre>../willow-servers/src/main/resources/webapp/images</pre>, tested and
checked into git.

## Note on originals ##

There is no reason to check in unoptimized svg, so before you add your icon,
please run it through for example <pre>scour</pre>.
