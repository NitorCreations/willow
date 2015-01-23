package com.nitorcreations.willow.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.nitorcreations.willow.utils.MergeableProperties;

@Mojo(name = "properties", defaultPhase = LifecyclePhase.GENERATE_TEST_RESOURCES, threadSafe = true)
public class PropertiesMojo extends AbstractMojo {
  @Parameter(defaultValue = "${project.build.directory}/application.properties", property = "outputFile", required = true)
  private File outputFile;
  @Parameter(required = false)
  private String[] prefixes;
  @Parameter(defaultValue = "root.properties", property = "rootProperties", required = true)
  private String rootProperties;
  @Parameter(defaultValue = "${project}", required = true)
  private MavenProject project;

  public void execute() throws MojoExecutionException {
    ensureDir(outputFile.getParentFile());
    if (prefixes == null || prefixes.length == 0) {
      prefixes = new String[] { "file:" + project.getBuild().getTestOutputDirectory() + "/" };
    }
    Properties tmp = new Properties();
    tmp.putAll(project.getProperties());
    for (Entry<Object, Object> next : project.getProperties().entrySet()) {
      String key = (String) next.getKey();
      String value = System.getProperty(key);
      if (value != null) {
        tmp.put(key, value);
        if (!"false".equalsIgnoreCase(System.getProperty(key + ".readonly"))) {
          tmp.put(key + ".readonly", "true");
        }
      }
    }
    MergeableProperties p = new MergeableProperties(prefixes);
    p.merge(tmp, rootProperties);
    try (OutputStream out = new FileOutputStream(outputFile)) {
      p.store(out, String.format("Artifact properties for %s", project.getArtifact().toString()));
      out.flush();
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to write merge properties", e);
    }
  }

  private static void ensureDir(File dir) throws MojoExecutionException {
    if (dir.exists() && !dir.isDirectory()) {
      throw new MojoExecutionException(String.format("%s exists and is not a directory", dir.getAbsolutePath()));
    }
    if (!dir.exists() && !dir.mkdirs()) {
      throw new MojoExecutionException(String.format("Failed to create directory %s", dir.getAbsolutePath()));
    }
  }
}
