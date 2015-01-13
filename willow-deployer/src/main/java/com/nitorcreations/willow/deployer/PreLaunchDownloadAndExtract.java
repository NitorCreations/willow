package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_DOWNLOAD_DIRECTORY;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD_URL;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_DOWNLOAD_FINALPATH;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_DOWNLOAD_MD5;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_DOWNLOAD_IGNORE_MD5;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_FILTER_GLOB;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_GLOB;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_ROOT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_SKIP_GLOB;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_OVERWRITE;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_WORKDIR;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
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
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import com.nitorcreations.willow.utils.MD5SumInputStream;

public class PreLaunchDownloadAndExtract implements Callable<Integer> {
	private final Properties properties;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	public PreLaunchDownloadAndExtract(Properties properties) {
		this.properties = properties;
	}
	public Integer call() {
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
		int downloads = 0;
		int i=0;
		String nextUrl=null;
		List<Future<Boolean>> futures = new ArrayList<>();
		while (null != (nextUrl = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + "[" + i + "]"))) {
			final String index = "[" + i + "]";
			Future<Boolean> next = executor.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					UrlDownloader dwn = new UrlDownloader(properties, index, getMd5(properties, index));
					return new Extractor(properties, index, PROPERTY_KEY_PREFIX_DOWNLOAD_URL, dwn.call()).call();
				}
			});
			futures.add(next);
			i++;
		}
		i = 0;
		while (null != (nextUrl = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT + "[" + i + "]"))) {
			final String index = "[" + i + "]";
			Future<Boolean> next = executor.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					UrlDownloader dwn = new UrlDownloader(properties, index, getMd5(properties, index));
					return new Extractor(properties, index, PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT, dwn.call()).call();
				}
			});
			futures.add(next);
			i++;
		}
		boolean failures = false;
		for (Future<Boolean> next : futures) {
			try {
				downloads++;
				if (!next.get()) failures = true;
			} catch (InterruptedException | ExecutionException e) {
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
