package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.FileUtil.createDir;
import static com.nitorcreations.willow.deployer.FileUtil.getFileName;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DOWNLOAD_DIRECTORY;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DOWNLOAD_RETRIES;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD_URL;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_DOWNLOAD_FINALPATH;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_DOWNLOAD_IGNORE_MD5;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_RETRIES;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_WORKDIR;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Hex;

import com.nitorcreations.willow.utils.MD5SumInputStream;

public class UrlDownloader implements Callable<File> {
  private final Properties properties;
  private final String index;
  private final byte[] md5;
  private final Logger logger;
  private final String url;
  private final String fileName;

  public UrlDownloader(Properties properties, String index, byte[] md5) {
    this.properties = properties;
    this.index = index;
    this.md5 = md5;
    this.url = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index);
    int queryIndex = url.lastIndexOf("?");
    if (queryIndex < 0)
      queryIndex = url.length();
    fileName = getFileName(url);
    logger = Logger.getLogger(fileName);
  }

  @Override
  public File call() throws IOException {
    if (url == null)
      return null;
    int retries = Integer.parseInt(properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index + PROPERTY_KEY_SUFFIX_RETRIES, properties.getProperty(PROPERTY_KEY_DOWNLOAD_RETRIES, "3")));
    int tryNo = 1;
    File workDir = new File(properties.getProperty(PROPERTY_KEY_WORKDIR, "."));
    File target = null;
    String downloadDir = properties.getProperty(PROPERTY_KEY_DOWNLOAD_DIRECTORY);
    while (tryNo <= retries) {
      try {
        URLConnection conn = new URL(url).openConnection();
        String finalPath = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index + PROPERTY_KEY_SUFFIX_DOWNLOAD_FINALPATH);
        if (finalPath != null) {
          target = new File(finalPath);
          if (!target.isAbsolute()) {
            target = new File(workDir, finalPath).getCanonicalFile();
          }
          File finalDir = target.getParentFile();
          if (!createDir(finalDir)) {
            throw new IOException("Failed to create final download directory " + finalDir.getAbsolutePath());
          }
        } else if (downloadDir != null) {
          File downloadDirFile = new File(downloadDir);
          if (!downloadDirFile.isAbsolute()) {
            downloadDirFile = new File(workDir, downloadDir).getCanonicalFile();
          }
          if (!createDir(downloadDirFile)) {
            throw new IOException("Failed to create final download directory " + downloadDirFile.getAbsolutePath());
          }
          target = new File(downloadDirFile, fileName);
        } else {
          target = File.createTempFile("depl.dwnld", fileName);
          target.deleteOnExit();
        }
        try (InputStream bIn = new BufferedInputStream(conn.getInputStream(), FileUtil.BUFFER_LEN)) {
          InputStream in = null;
          MD5SumInputStream md5in = null;
          if (md5 != null) {
            md5in = new MD5SumInputStream(bIn);
            in = md5in;
          } else {
            in = bIn;
          }
          FileUtil.copy(in, target);
          if (md5 != null && Arrays.equals(md5, md5in.digest())) {
            String md5str = Hex.encodeHexString(md5);
            logger.info(url + " md5 sum ok " + md5str);
            File md5file = new File(target.getParentFile(), target.getName() + ".md5");
            try (OutputStream out = new FileOutputStream(md5file)) {
              out.write((md5str + "  " + target.getName() + "\n").getBytes());
              out.flush();
            }
          } else if (!"true".equalsIgnoreCase(properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index + PROPERTY_KEY_SUFFIX_DOWNLOAD_IGNORE_MD5))) {
            throw new IOException("MD5 Sum does not match for " + url + " - expected: " + Hex.encodeHexString(md5) + " got: " + Hex.encodeHexString(md5in.digest()));
          }
        }
        break;
      } catch (IOException | NoSuchAlgorithmException e) {
        tryNo++;
        target = null;
        LogRecord rec = new LogRecord(Level.WARNING, "Failed to download and extract " + url);
        rec.setThrown(e);
        logger.log(rec);
        if (tryNo <= retries) {
          logger.log(Level.WARNING, "Retrying (" + tryNo + ")s");
        }
      }
    }
    if (target == null)
      throw new IOException("Failed to download " + url);
    return target;
  }
}
