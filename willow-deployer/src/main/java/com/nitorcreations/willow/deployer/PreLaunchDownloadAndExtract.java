package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD_URL;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_DOWNLOAD_MD5;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_DOWNLOAD_IGNORE_MD5;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class PreLaunchDownloadAndExtract implements Callable<Integer> {
  private final Properties properties;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public PreLaunchDownloadAndExtract(Properties properties) {
    this.properties = properties;
  }

  public Integer call() {
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    int downloads = 0;
    List<Future<Boolean>> futures = new ArrayList<>();
    for (int i = 0; null != (properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + "[" + i + "]")); i++) {
      final String index = "[" + i + "]";
      Future<Boolean> next = executor.submit(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          byte[] md5 = getMd5(properties, index);
          UrlDownloader dwn = new UrlDownloader(properties, index, md5);
          File downloaded = dwn.call();
          if (downloaded != null && downloaded.exists()) {
            return new Extractor(properties, index, PROPERTY_KEY_PREFIX_DOWNLOAD_URL, downloaded).call();
          } else {
            return false;
          }
        }
      });
      futures.add(next);
    }
    for (int i = 0; null != (properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT + "[" + i + "]")); i++) {
      final String index = "[" + i + "]";
      Future<Boolean> next = executor.submit(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          UrlDownloader dwn = new UrlDownloader(properties, index, getMd5(properties, index));
          return new Extractor(properties, index, PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT, dwn.call()).call();
        }
      });
      futures.add(next);
    }
    boolean failures = false;
    for (Future<Boolean> next : futures) {
      try {
        downloads++;
        if (!next.get())
          failures = true;
      } catch (InterruptedException | ExecutionException e) {
        LogRecord rec = new LogRecord(Level.WARNING, "Failed download and extract");
        rec.setThrown(e);
        logger.log(rec);
      }
    }
    executor.shutdownNow();
    return failures ? -downloads : downloads;
  }

  private byte[] getMd5(Properties properties, String index) throws IOException {
    byte[] md5 = null;
    String url = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index);
    String urlMd5 = url + ".md5";
    String propMd5 = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index + PROPERTY_KEY_SUFFIX_DOWNLOAD_MD5);
    if (propMd5 != null) {
      try {
        md5 = Hex.decodeHex(propMd5.toCharArray());
      } catch (DecoderException e) {
        logger.warning("Invalid md5sum " + propMd5);
        throw new IOException("Failed to download " + url, e);
      }
    } else {
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        URLConnection conn = new URL(urlMd5).openConnection();
        FileUtil.copy(conn.getInputStream(), out);
        String md5Str = new String(out.toByteArray(), 0, 32);
        md5 = Hex.decodeHex(md5Str.toCharArray());
      } catch (IOException | DecoderException e) {
        LogRecord rec = new LogRecord(Level.INFO, "No md5 sum available" + urlMd5);
        logger.log(rec);
        if (!"true".equalsIgnoreCase(properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index + PROPERTY_KEY_SUFFIX_DOWNLOAD_IGNORE_MD5))) {
          throw new IOException("Failed to get a valid md5sum for " + url, e);
        }
      }
    }
    return md5;
  }
}
