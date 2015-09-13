package com.nitorcreations.willow.download;

import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_LOCAL_REPOSITORY;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_REMOTE_REPOSITORY;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

public class AetherDownloader implements Callable<File> {
  private String localRepo;
  private String remoteRepo;
  private LocalRepository local;
  private RemoteRepository remote;
  private RepositorySystem system;
  private final String artifact;

  public AetherDownloader() {
    this.artifact = null;
  }

  public AetherDownloader(Properties properties) {
    setProperties(properties);
    this.artifact = properties.getProperty("");
  }
  public File downloadArtifact(String artifactCoords) {
    Dependency dependency = new Dependency(new DefaultArtifact(artifactCoords), "runtime");
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, local));
    ArtifactRequest req = new ArtifactRequest();
    req.setArtifact(dependency.getArtifact());
    req.addRepository(remote);
    File rootJar = null;
    try {
      ArtifactResult result = system.resolveArtifact(session, req);
      rootJar = result.getArtifact().getFile();
    } catch (ArtifactResolutionException e) {
      throw new RuntimeException("Failed to resolve " + artifactCoords, e);
    }
    return rootJar;
  }
  public String downloadTransitive(String artifactCoords) {
    Dependency dependency = new Dependency(new DefaultArtifact(artifactCoords), "runtime");
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(dependency);
    collectRequest.addRepository(remote);
    DependencyNode node;
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, local));
    try {
      node = system.collectDependencies(session, collectRequest).getRoot();
      DependencyRequest dependencyRequest = new DependencyRequest();
      dependencyRequest.setRoot(node);
      system.resolveDependencies(session, dependencyRequest);
      PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
      node.accept(nlg);
      return nlg.getClassPath();
    } catch (DependencyResolutionException | DependencyCollectionException e) {
      throw new RuntimeException("Failed to resolve (transitively) " + artifactCoords, e);
    }
  }
  public void setProperties(Properties properties) {
    localRepo = System.getenv(ENV_DEPLOYER_LOCAL_REPOSITORY);
    if (localRepo == null) {
      localRepo = System.getProperty("user.home") + File.separator + ".deployer" + File.separator + "repository";
    }
    remoteRepo = properties.getProperty(PROPERTY_KEY_REMOTE_REPOSITORY, "http://localhost:5120/maven");
    system = GuiceRepositorySystemFactory.newRepositorySystem();
    local = new LocalRepository(localRepo);
    remote = new RemoteRepository.Builder("deployer", "default", remoteRepo).build();
  }
  @Override
  public File call() throws Exception {
    if (artifact == null) {
      return null;
    }
    return downloadArtifact(artifact);
  }
}
