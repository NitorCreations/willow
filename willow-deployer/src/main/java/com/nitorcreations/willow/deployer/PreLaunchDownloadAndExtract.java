package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD_URL;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nitorcreations.willow.download.Extractor;
import com.nitorcreations.willow.download.UrlDownloader;
import com.nitorcreations.willow.protocols.property.PropertyUrlConnection;
import com.nitorcreations.willow.utils.FileUtil;
import com.nitorcreations.willow.utils.MergeableProperties;

@SuppressWarnings("PMD.TooManyStaticImports")
public class PreLaunchDownloadAndExtract implements Callable<Integer> {
  private final MergeableProperties properties;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public PreLaunchDownloadAndExtract(MergeableProperties properties) {
    this.properties = properties;
  }

  @Override
  public Integer call() {
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    int downloads = 0;
    List<Future<Boolean>> futures = new ArrayList<>();
    int i=0;
    for (final MergeableProperties downloadProperties : properties.getPrefixedList(PROPERTY_KEY_PREFIX_DOWNLOAD_URL)) {
      Main.addSharedLaunchAndDownloadProperties(downloadProperties, properties, i);
      Future<Boolean> next = executor.submit(getUrlCallable(downloadProperties));
      futures.add(next);
      i++;
    }
    for (final MergeableProperties downloadProperties : properties.getPrefixedList(PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT)) {
      Main.addSharedLaunchAndDownloadProperties(downloadProperties, properties, i);
      Future<Boolean> next = executor.submit(getArtifactCallable(downloadProperties));
      futures.add(next);
    }
    for (final MergeableProperties downloadProperties : properties.getPrefixedList(PROPERTY_KEY_PREFIX_DOWNLOAD)) {
      Main.addSharedLaunchAndDownloadProperties(downloadProperties, properties, i);
      if (downloadProperties.getProperty("url") != null) {
        Future<Boolean> next = executor.submit(getUrlCallable(downloadProperties));
        futures.add(next);
      } else if (downloadProperties.getProperty("artifact") != null) {
        Future<Boolean> next = executor.submit(getArtifactCallable(downloadProperties));
        futures.add(next);
      }
    }
    boolean failures = false;
    for (Future<Boolean> next : futures) {
      try {
        downloads++;
        if (!next.get()) {
          failures = true;
        }
      } catch (InterruptedException | ExecutionException e) {
        logger.log(Level.WARNING, "Failed download and extract", e);
      }
    }
    executor.shutdownNow();
    return failures ? -downloads : downloads;
  }

  private Callable<Boolean> getUrlCallable(Properties downloadProperties) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        try {
          PropertyUrlConnection.currentProperties.set(properties);
          byte[] md5 = FileUtil.getMd5(downloadProperties);
          UrlDownloader dwn = new UrlDownloader(downloadProperties, md5);
          File downloaded = dwn.call();
          if (downloaded != null && downloaded.exists()) {
            downloadProperties.putAll(properties);
            return new Extractor(downloadProperties, downloaded).call();
          } else {
            return false;
          }
        } finally {
          PropertyUrlConnection.currentProperties.set(null);
        }
      }
    };
  }
  private Callable<Boolean> getArtifactCallable(Properties downloadProperties) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        UrlDownloader dwn = new UrlDownloader(downloadProperties, FileUtil.getMd5(downloadProperties));
        downloadProperties.putAll(properties);
        File target = dwn.call();
        if (target != null) {
          return new Extractor(downloadProperties, target).call();
        } else {
          return false;
        }
      }
    };
  }
}
