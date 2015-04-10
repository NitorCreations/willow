package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.ENV_DEPLOYER_HOME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hyperic.sigar.Humidor;
import org.hyperic.sigar.SigarProxy;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.nitorcreations.willow.deployer.download.FileUtil;
import com.nitorcreations.willow.messages.WebSocketTransmitter;

public class DeployerModule extends AbstractModule {
  @Override
  protected void configure() {
    extractNativeLib();
    bind(WebSocketTransmitter.class).toInstance(new WebSocketTransmitter());
    bind(ExecutorService.class).toInstance(Executors.newCachedThreadPool());
    bind(SigarProxy.class).toProvider(new Provider<SigarProxy>() {
      Humidor humidor = Humidor.getInstance();
      @Override
      public SigarProxy get() {
        return humidor.getSigar();
      }
    });
  }
  protected void extractNativeLib() {
    String deployerHome = System.getenv(ENV_DEPLOYER_HOME);
    if (deployerHome == null) {
      deployerHome = System.getProperty("user.home") + File.separator + ".deployer" + File.separator + "repository";
    }
    File libDir = new File(new File(deployerHome), "lib");
    System.setProperty("java.library.path", libDir.getAbsolutePath());
    String arch = System.getProperty("os.arch");
    String os = System.getProperty("os.name").toLowerCase();
    StringBuilder libName = new StringBuilder();
    if (os.contains("win")) {
      libName.append("sigar-");
    } else {
      libName.append("libsigar-");
    }
    if (os.contains("mac") || os.contains("darwin")) {
        if (arch.contains("64")) {
            libName.append("universal64");
        } else {
            libName.append("universal");
        }
    } else {
        libName.append(arch);
    }
    if (os.contains("win")) {
      libName.append("-winnt").append(".dll");
    } else if (os.contains("mac") || os.contains("darwin")) {
      libName.append("-macosx.dylib");
    } else if (os.contains("sunos")) {
      libName.append("-solaris.so");
    } else {
      libName.append("-").append(os).append(".so");
    }
    File libFile = new File(libDir, libName.toString());
    if (!(libFile.exists() && libFile.canExecute())) {
      InputStream lib = Main.class.getClassLoader().getResourceAsStream(libName.toString());
      FileUtil.createDir(libDir);
      if (lib != null) {
        try (OutputStream out = new FileOutputStream(libFile)) {
          byte[] buffer = new byte[1024];
          int len;
          while ((len = lib.read(buffer)) != -1) {
            out.write(buffer, 0, len);
          }
          libFile.setExecutable(true, false);
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          try {
            lib.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      } else {
        throw new RuntimeException("Failed to find " + libName);
      }
    }
  }

}
