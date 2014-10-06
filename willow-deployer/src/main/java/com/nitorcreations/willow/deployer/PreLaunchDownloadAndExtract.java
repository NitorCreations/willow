package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.*;

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
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
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
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import com.nitorcreations.willow.utils.MD5SumInputStream;

public class PreLaunchDownloadAndExtract {
	private final ArchiveStreamFactory factory = new ArchiveStreamFactory();
	private final CompressorStreamFactory cfactory = new CompressorStreamFactory();
	private static Map<Integer, PosixFilePermission> perms = new HashMap<Integer, PosixFilePermission>();
	private Logger logger = Logger.getLogger(this.getClass().getName());
	static {
		perms.put(0001, PosixFilePermission.OTHERS_EXECUTE);
		perms.put(0002, PosixFilePermission.OTHERS_WRITE);
		perms.put(0004, PosixFilePermission.OTHERS_READ);
		perms.put(0010, PosixFilePermission.GROUP_EXECUTE);
		perms.put(0020, PosixFilePermission.GROUP_WRITE);
		perms.put(0040, PosixFilePermission.GROUP_READ);
		perms.put(0100, PosixFilePermission.OWNER_EXECUTE);
		perms.put(0200, PosixFilePermission.OWNER_WRITE);
		perms.put(0400, PosixFilePermission.OWNER_READ);
	}
	public void execute(Properties properties) {
		Map<String, String> replaceTokens = new HashMap<>();
		for (Entry<Object,Object> nextEntry : properties.entrySet()) {
			replaceTokens.put("${" + nextEntry.getKey() + "}", (String)nextEntry.getValue());
			replaceTokens.put("@" + nextEntry.getKey() + "@", (String)nextEntry.getValue());
		}
		int i = 0;
		int retries = 0;
		boolean lastSuccess = true;
		while (lastSuccess) {
			try {
				if (retries > 3) throw new RuntimeException("Download failed");
				lastSuccess = downloadUrl("[" + i++ + "]", properties, replaceTokens);
				retries = 0;
			} catch (IOException e) {
				lastSuccess = true;
				i--;
				retries++;
			}
		}
		i = 0;
		lastSuccess = true;
		while (lastSuccess) {
			try {
				if (retries > 3) throw new RuntimeException("Download failed");
				lastSuccess = downloadArtifact("[" + i++ + "]", properties, replaceTokens);
				retries = 0;
			} catch (IOException e) {
				lastSuccess = true;
				i--;
				retries++;
			}
		}
	}

	private boolean downloadUrl(String index, Properties properties, Map<String, String> replaceTokens) throws IOException {
		String url = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index);
		if (url == null) return false;
		String urlMd5 = url + ".md5";
		byte[] md5 = null;
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
				LogRecord rec = new LogRecord(Level.WARNING, "Failed to get md5" + urlMd5);
				rec.setThrown(e);
				logger.log(rec);
			}
		}
		String extractRoot = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index + PROPERTY_KEY_SUFFIX_EXTRACT_ROOT, 
				properties.getProperty(PROPERTY_KEY_WORKDIR, ".")) ;
		String extractGlob = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index + PROPERTY_KEY_SUFFIX_EXTRACT_GLOB) ;
		String skipExtractGlob = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index + PROPERTY_KEY_SUFFIX_EXTRACT_SKIP_GLOB);
		String filterGlob = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + index + PROPERTY_KEY_SUFFIX_EXTRACT_FILTER_GLOB);
		try {
			URLConnection conn = new URL(url).openConnection();
			String fileName = FileUtil.getFileName(url);
			File target = null;
			if (properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_FINALPATH) != null) {
				target = new File(new File(properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_FINALPATH)), fileName);
			} else {
				target = File.createTempFile(fileName, ".download");
			}
			InputStream in;
			MD5SumInputStream md5in = null;
			if (md5 != null) {
				md5in = new MD5SumInputStream(conn.getInputStream());
				in = md5in;
			} else {
				in = conn.getInputStream();
			}
			FileUtil.copy(in, target);
			if (extractGlob != null || skipExtractGlob != null) {
				extractFile(target, replaceTokens, extractRoot, extractGlob, skipExtractGlob, filterGlob);
			}
			if (md5 != null && Arrays.equals(md5, md5in.digest())) {
				logger.info(url + " md5 sum ok " + Hex.encodeHexString(md5));
			} else if (md5 != null){
				throw new IOException("MD5 Sum does not match " + Hex.encodeHexString(md5) + " != " + Hex.encodeHexString(md5in.digest()));
			}
		} catch (IOException | CompressorException | ArchiveException 
				| NoSuchAlgorithmException e) {
			LogRecord rec = new LogRecord(Level.WARNING, "Failed to download and extract " + url);
			rec.setThrown(e);
			logger.log(rec);
			throw new IOException("Failed to download url " + url, e);
		}
		return true;
	}
	private void extractFile(File archive, Map<String, String> replaceTokens, String root, 
			String extractGlob, String skipExtractGlob, String filterGlob) throws CompressorException, IOException, ArchiveException {
		String lcFileName = archive.getName().toLowerCase();
		Set<PathMatcher> extractMatchers = getGlobMatchers(extractGlob);
		Set<PathMatcher> skipMatchers = getGlobMatchers(skipExtractGlob);
		Set<PathMatcher> filterMatchers = getGlobMatchers(filterGlob);
		InputStream in = new BufferedInputStream(new FileInputStream(archive), 8 * 1024);
		if (lcFileName.endsWith("z") ||	lcFileName.endsWith("bz2") || lcFileName.endsWith("lzma") ||
				lcFileName.endsWith("arj") || lcFileName.endsWith("deflate")) {
			in = cfactory.createCompressorInputStream(in);
		}
		extractArchive(factory.createArchiveInputStream(in), new File(root), replaceTokens, extractMatchers, skipMatchers, filterMatchers);
	}
	private boolean downloadArtifact(String index, Properties properties, Map<String, String> replaceTokens) throws IOException {
		String artifact = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT);
		if (artifact == null) return false;
		AetherDownloader downloader = new AetherDownloader();
		downloader.setProperties(properties);
		File artifactFile = downloader.downloadArtifact(artifact);
		String extractRoot = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT + index + PROPERTY_KEY_SUFFIX_EXTRACT_ROOT, 
				properties.getProperty(PROPERTY_KEY_WORKDIR, ".")) ;
		String extractGlob = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT + index + PROPERTY_KEY_SUFFIX_EXTRACT_GLOB) ;
		String skipExtractGlob = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT + index + PROPERTY_KEY_SUFFIX_EXTRACT_SKIP_GLOB);
		String filterGlob = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT + index + PROPERTY_KEY_SUFFIX_EXTRACT_FILTER_GLOB);
		try {
			if (extractGlob != null || skipExtractGlob != null) {
				extractFile(artifactFile, replaceTokens, extractRoot, extractGlob, skipExtractGlob, filterGlob);
			}
		} catch (CompressorException | IOException | ArchiveException e) {
			LogRecord rec = new LogRecord(Level.WARNING, "Failed to download and extract artifact " + artifact);
			rec.setThrown(e);
			logger.log(rec);
			throw new IOException("Failed to download artifact" + artifact, e);
		}
		return true;
	}
	private boolean globMatches(String path, Set<PathMatcher> matchers) {
		for (PathMatcher next : matchers) {
			if (next.matches(Paths.get(path))) return true;
		}
		return false;
	}
	private Set<PathMatcher> getGlobMatchers(String expressions) {
		Set<PathMatcher> matchers = new LinkedHashSet<>();
		if (expressions == null || expressions.isEmpty()) return matchers;
		FileSystem def = FileSystems.getDefault();
		for (String next : expressions.split("\\|")) {
			String trimmed = next.trim();
			if (!trimmed.isEmpty()) {
				matchers.add(def.getPathMatcher("glob:"  + trimmed));
			}
		}
		return matchers;
	}
	public Set<PosixFilePermission> getPermissions(int mode) {
		Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>();
		for (int mask : perms.keySet()) {
			if (mask == (mode & mask)) {
				permissions.add(perms.get(mask));
			}
		}
		return permissions;
	}
	private void extractArchive(ArchiveInputStream is, File destFolder, Map<String, String> replaceTokens, Set<PathMatcher> extractMatchers, Set<PathMatcher> skipMatchers, Set<PathMatcher> filterMatchers) throws IOException {
		try {
			ArchiveEntry entry;
			while((entry =  is.getNextEntry()) != null) {
				File dest = new File(destFolder, entry.getName());
				if (globMatches(entry.getName(), extractMatchers) && !globMatches(entry.getName(), skipMatchers)) {
					if (entry.isDirectory()) {
						dest.mkdirs();
					} else {
						dest.getParentFile().mkdirs();
						if (globMatches(entry.getName(), filterMatchers)) {
							FileUtil.filterStream(is, dest, replaceTokens);
						} else {
							FileUtil.copy(is, dest);
						}
					}
					Set<PosixFilePermission> permissions = getPermissions(getMode(entry));
					Path destPath = Paths.get(dest.getAbsolutePath());
					Files.setPosixFilePermissions(destPath, permissions);
				}
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				throw e;
			}
		}
	}
	private int getMode(ArchiveEntry entry) {
		Method m = null;
		try {
			m = entry.getClass().getMethod("getMode"); 
		} catch (NoSuchMethodException | SecurityException e) {
		}
		if (m == null) {
			if (entry instanceof ZipArchiveEntry) {
				int ret = (int) ((((ZipArchiveEntry)entry).getExternalAttributes() >> 16) & 0xFFF);
				if (ret == 0) { return 0644; }
				else return ret;
			} else return 0664;
		} else {
			try {
				return (int)((Number)m.invoke(entry)).longValue() & 0xFFF;
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				return 0664;
			}
		}
	}
}
