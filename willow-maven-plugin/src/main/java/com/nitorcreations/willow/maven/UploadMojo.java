package com.nitorcreations.willow.maven;

import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_DEPLOYER_NAME;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_DOWNLOAD_DIRECTORY;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_DOWNLOAD_RETRIES;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_REMOTE_REPOSITORY;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_SUFFIX_DOWNLOAD_FINALPATH;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_WORKDIR;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
import org.apache.maven.repository.RepositorySystem;

import com.google.gson.Gson;
import com.nitorcreations.willow.download.UrlDownloader;
import com.nitorcreations.willow.utils.FileUtil;
import com.nitorcreations.willow.utils.MavenFormatter;
import com.nitorcreations.willow.utils.ProxyUtils;
import com.nitorcreations.willow.utils.SimpleFormatter;

import at.spardat.xma.xdelta.JarDelta;

@Mojo(name = "upload", threadSafe = true, aggregator = true)
public class UploadMojo extends AbstractMojo {

  @Component
  protected RepositorySystem repoSystem;


  @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
  protected  ArtifactRepository local;

  @Parameter( required = true)
  protected Dependency propertiesArtifact;

  @Parameter( required = false)
  protected List<Dependency> artifacts;

  @Parameter( required = false, readonly = true )
  protected List<Properties> downloads;
  
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
  public UploadMojo() {
    super();
    setupLogging();
  }
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File dir = Paths.get(local.getBasedir(), project.getGroupId().replaceAll("\\.", "/")).toFile();
    File target = new File(dir, systemName + "-" + systemVersion + ".zip");
    getLog().info("Creating system archive: " + target.getAbsolutePath());
    try (ZipArchiveOutputStream zipFile = new ZipArchiveOutputStream(new FileOutputStream(target));){
      copyEntry("properties.jar", propertiesArtifact, zipFile);
      for (Dependency next : artifacts) {
        String nextName = next.getArtifactId();
        if (next.getClassifier() != null && !next.getClassifier().isEmpty()) {
          nextName += "-" + next.getClassifier();
        }
        copyEntry(nextName +".jar", next, zipFile);
      }
      for (Properties next : downloads) {
        String url = next.getProperty("url");
        if (url == null) {
          throw new MojoExecutionException("Download with no url: " + next.toString());
        }
        String name = next.getProperty("finalName");
        if (name == null) {
          name = FileUtil.getFileName(url); 
        }
        File dwnldTarget = new File(dir, name);
        next.setProperty(PROPERTY_KEY_SUFFIX_DOWNLOAD_FINALPATH, dwnldTarget.getAbsolutePath());
        dwnldTarget = new UrlDownloader(next, FileUtil.getMd5(next)).call();
        zipFile.putArchiveEntry(new ZipArchiveEntry(name));
        try (FileInputStream in = new FileInputStream(dwnldTarget)) {
          FileUtil.copy(in, zipFile);
          zipFile.closeArchiveEntry();
        }
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
        File nextFile = new File(dir, nextVersionFileName);
        if (nextFile.exists()) {
          diffedVersion = nextVersion;
          diffFile = new File(dir, systemName + "-" + nextVersion + "-" + systemVersion + ".xd");
          getLog().info("Creating diff: " + diffFile.getAbsolutePath());
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
        HttpURLConnection conn = getUrlConnection(deploymentUrl + systemName + "/" + systemVersion + "?diff=" + diffedVersion);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/zip");
        conn.setDoOutput(true);
        try (OutputStream out = conn.getOutputStream();
            InputStream in = new FileInputStream(diffFile)) {
          getLog().info("Uploading diff: " + diffFile.getAbsolutePath());
          FileUtil.copy(in, out);
          diffUploaded = true;
          if (conn.getResponseCode() >= 300) {
            throw new MojoExecutionException("Failed to upload system package");
          }
        }
      } catch (URISyntaxException | IOException e) {
        getLog().warn("Failed to upload diff", e);
      }
    }
    if (!diffUploaded) {
      try {
        HttpURLConnection conn = getUrlConnection(deploymentUrl + systemName + "/" + systemVersion);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/zip");
        conn.setDoOutput(true);
        try (OutputStream out = conn.getOutputStream();
            InputStream in = new FileInputStream(target)) {
          getLog().info("Uploading full system: " + target.getAbsolutePath());
          FileUtil.copy(in, out);
          diffUploaded = true;
          if (conn.getResponseCode() >= 300) {
            throw new MojoExecutionException("Failed to upload system package");
          }
        }
      } catch (URISyntaxException | IOException e) {
        throw new MojoExecutionException("Failed to upload release", e);
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
    if (nextFile == null || !nextFile.exists()) {
      throw new IOException(dependency.toString() + " not found");
    }
    try (FileInputStream in = new FileInputStream(nextFile)) {
      FileUtil.copy(in, zipFile);
      zipFile.closeArchiveEntry();
    }
  }
  protected HttpURLConnection getUrlConnection(String url) throws IOException, URISyntaxException {
    URI uri = new URI(url);
    List<Proxy> l = ProxyUtils.resolveProxies(System.getProperty("proxy.autoconf"), proxy, uri);
    if (l == null) {
      l = new ArrayList<Proxy>();
    }
    for (org.apache.maven.settings.Proxy next : session.getSettings().getProxies()) {
      Proxy.Type type  = Proxy.Type.HTTP;
      if (next.getProtocol().toUpperCase(Locale.ENGLISH).startsWith("SOCKS")) {
        type = Proxy.Type.SOCKS;
      }
      l.add(new Proxy(type, new InetSocketAddress(next.getHost(), next.getPort())));
    }
    return (HttpURLConnection) ProxyUtils.openConnection(uri, l);
  }
  public void setupLogging() {
    Logger rootLogger = Logger.getLogger("");
    for (Handler nextHandler : rootLogger.getHandlers()) {
      rootLogger.removeHandler(nextHandler);
    }
    Handler console = new ConsoleHandler();
    console.setFormatter(new MavenFormatter());
    rootLogger.addHandler(console);
    if (getLog().isDebugEnabled()) {
      console.setLevel(Level.FINER);
      rootLogger.setLevel(Level.FINER);
    } else {
      console.setLevel(Level.INFO);
      rootLogger.setLevel(Level.INFO);
    }
  }
}
