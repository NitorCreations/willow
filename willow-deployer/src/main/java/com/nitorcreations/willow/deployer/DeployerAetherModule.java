package com.nitorcreations.willow.deployer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.repository.internal.MavenAetherModule;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.classpath.ClasspathTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;

class DeployerAetherModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new MavenAetherModule());
    bind(RepositoryConnectorFactory.class).annotatedWith(Names.named("basic")).to(BasicRepositoryConnectorFactory.class);
    bind(TransporterFactory.class).annotatedWith(Names.named("file")).to(FileTransporterFactory.class);
    bind(TransporterFactory.class).annotatedWith(Names.named("http")).to(HttpTransporterFactory.class);
    bind(TransporterFactory.class).annotatedWith(Names.named("classpath")).to(ClasspathTransporterFactory.class);
  }

  @Provides
  @Singleton
  Set<RepositoryConnectorFactory> provideRepositoryConnectorFactories(@Named("basic") RepositoryConnectorFactory basic) {
    Set<RepositoryConnectorFactory> factories = new HashSet<RepositoryConnectorFactory>();
    factories.add(basic);
    return Collections.unmodifiableSet(factories);
  }

  @Provides
  @Singleton
  Set<TransporterFactory> provideTransporterFactories(@Named("file") TransporterFactory file, @Named("http") TransporterFactory http) {
    Set<TransporterFactory> factories = new HashSet<TransporterFactory>();
    factories.add(file);
    factories.add(http);
    return Collections.unmodifiableSet(factories);
  }
}
