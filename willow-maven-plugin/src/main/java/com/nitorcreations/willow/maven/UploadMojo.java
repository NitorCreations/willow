package com.nitorcreations.willow.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBElement.GlobalScope;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.LocalArtifactRepository;
import org.apache.maven.repository.RepositorySystem;

import com.google.gson.Gson;
import com.nitorcreations.willow.utils.FileUtil;
import com.nitorcreations.willow.utils.ProxyUtils;

import at.spardat.xma.xdelta.JarDelta;

@Mojo(name = "upload", threadSafe = true, aggregator = true)
public class UploadMojo extends AbstractMojo {

  @Component
  protected RepositorySystem repoSystem;


  @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
  protected  LocalArtifactRepository local;

  @Parameter( required = true)
  protected Dependency propertiesArtifact;

  @Parameter( required = true)
  protected List<Dependency> artifacts;

  @Parameter
  protected List<String> urls;

  @Parameter(defaultValue = "${project.artifactId}", required = true)
  protected String systemName;

  @Parameter(defaultValue = "${project.version}.${env.BUILD_NUMBER}", required = true)
  protected String systemVersion;

  @Parameter(defaultValue = "10")
  protected int keepVersions;

  @Parameter(defaultValue = "${project}")
  protected MavenProject project;

  @Parameter( defaultValue = "${session}", readonly = true )
  private MavenSession session;

  @Parameter(required = true)
  protected String deploymentUrl;

  @Parameter(required = false)
  protected String proxy;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File dir = Paths.get(local.getBasedir(), project.getGroupId().replaceAll("\\.", "/")).toFile();
    File target = new File(dir, systemName + "-" + systemVersion + ".zip");
    try (ZipArchiveOutputStream zipFile = new ZipArchiveOutputStream(new FileOutputStream(target));){
      copyEntry("properties.jar", propertiesArtifact, zipFile);
      for (Dependency next : artifacts) {
        copyEntry(next.getArtifactId() + ".jar", next, zipFile);
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to create zip file" ,e);
    }
    if (!deploymentUrl.endsWith("/")) {
      deploymentUrl += "/";
    }
    String[] versions = null;
    try (InputStream in = getUrlConnection(deploymentUrl + systemName + "/diffcandidates").getInputStream()) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      FileUtil.copy(in, out);
      String result = out.toString("UTF-8");
      versions = new Gson().fromJson(result, String[].class);
    } catch (URISyntaxException | IOException e) {
      getLog().warn("Failed to get diff candidates from deployment server", e);
    }
    File diffFile = null;
    String diffedVersion = null;
    if (versions != null && versions.length > 0) {
      for (String nextVersion : versions) {
        String nextVersionFileName = systemName + "-" + nextVersion + ".zip";
        File nextFile = new File(nextVersionFileName);
        if (nextFile.exists()) {
          diffedVersion = nextVersion;
          diffFile = new File(dir, systemName + "-" + nextVersion + "-" + systemVersion + ".xd");
          try (ZipArchiveOutputStream output = new ZipArchiveOutputStream(new FileOutputStream(diffFile))) {
            new JarDelta().computeDelta(nextFile.getName(), target.getName(), 
                new ZipFile(nextFile), new ZipFile(target), output);
          } catch (IOException e) {
            getLog().warn("Failed to get calculate diff", e);
            diffFile = null;
          }
          break;
        }
      }
    }
    boolean diffUploaded = false;
    if (diffFile != null) {
      try {
        URLConnection conn = getUrlConnection(deploymentUrl + systemName + "/" + systemVersion + "?diff=" + diffedVersion);
        conn.setDoOutput(true);
        try (OutputStream out = conn.getOutputStream();
            InputStream in = new FileInputStream(diffFile)) {
          FileUtil.copy(in, out);
          diffUploaded = true;
        }
      } catch (URISyntaxException | IOException e) {
        getLog().warn("Failed to upload diff", e);
      }
    }
    if (!diffUploaded) {
      try {
        URLConnection conn = getUrlConnection(deploymentUrl + systemName + "/" + systemVersion);
        conn.setDoOutput(true);
        try (OutputStream out = conn.getOutputStream();
            InputStream in = new FileInputStream(target)) {
          FileUtil.copy(in, out);
          diffUploaded = true;
        }
      } catch (URISyntaxException | IOException e) {
        getLog().warn("Failed to upload release", e);
      }
    }
  }
  protected void copyEntry(String entryName, Dependency dependency, ZipArchiveOutputStream zipFile) throws IOException {
    zipFile.putArchiveEntry(new ZipArchiveEntry(entryName));
    Artifact nextArtifact = repoSystem.createArtifactWithClassifier(dependency.getGroupId(), 
        dependency.getArtifactId(), dependency.getVersion(), dependency.getType(), dependency.getClassifier());
    ArtifactResolutionRequest req = new ArtifactResolutionRequest();
    req.setArtifact(nextArtifact);
    ArtifactResolutionResult resolved = repoSystem.resolve(req);
    File nextFile = resolved.getArtifacts().iterator().next().getFile();
    try (FileInputStream in = new FileInputStream(nextFile)) {
      FileUtil.copy(in, zipFile);
      zipFile.closeArchiveEntry();
    }
  }
  protected URLConnection getUrlConnection(String url) throws IOException, URISyntaxException {
    URI uri = new URI(url);
    List<Proxy> l = ProxyUtils.resolveProxies(System.getProperty("proxy.autoconf"), proxy, uri);
    for (org.apache.maven.settings.Proxy next : session.getSettings().getProxies()) {
      Proxy.Type type  = Proxy.Type.HTTP;
      if (next.getProtocol().toUpperCase(Locale.ENGLISH).startsWith("SOCKS")) {
        type = Proxy.Type.SOCKS;
      }
      l.add(new Proxy(type, new InetSocketAddress(next.getHost(), next.getPort())));
    }
    return ProxyUtils.openConnection(uri, l);
  }
}
