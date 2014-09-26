import java.util.Properties

File propertiesFile = new File( basedir, "target/app.properties" );

assert propertiesFile.isFile()

Properties props  = new Properties();

propertiesFile.withInputStream { 
  stream -> props.load(stream) 
}
def config = new ConfigSlurper().parse(props)

assert config.foo.bar == "baz,foo/bar"
assert config.component.id == "webfront"