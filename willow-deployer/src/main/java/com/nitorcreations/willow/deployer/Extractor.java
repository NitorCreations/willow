package com.nitorcreations.willow.deployer;

import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_FILTER_GLOB;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_GLOB;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_OVERWRITE;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_ROOT;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_SKIP_GLOB;
import static com.nitorcreations.willow.deployer.PropertyKeys.PROPERTY_KEY_WORKDIR;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

public class Extractor implements Callable<Boolean> {

	private final Properties properties;
	private final String index;
	private final String prefix;
	private final File target;
	private final Logger logger;

	private final ArchiveStreamFactory factory = new ArchiveStreamFactory();
	private final CompressorStreamFactory cfactory = new CompressorStreamFactory();
	private static Map<Integer, PosixFilePermission> perms = new HashMap<Integer, PosixFilePermission>();
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

	public Extractor(Properties properties, String index, String prefix, File target) {
		this.properties = properties;
		this.index = index;
		this.prefix = prefix;
		this.target = target;
		logger = Logger.getLogger(target.getName());
	}
	@Override
	public Boolean call() {
		if (target == null) return false;
		File workDir = new File(properties.getProperty(PROPERTY_KEY_WORKDIR, "."));
		boolean overwrite = "false".equalsIgnoreCase(properties.getProperty(prefix + index + PROPERTY_KEY_SUFFIX_EXTRACT_OVERWRITE)) ? false : true;
		String extractRoot = properties.getProperty(prefix + index + PROPERTY_KEY_SUFFIX_EXTRACT_ROOT, workDir.getAbsolutePath()) ;
		String extractGlob = properties.getProperty(prefix + index + PROPERTY_KEY_SUFFIX_EXTRACT_GLOB) ;
		String skipExtractGlob = properties.getProperty(prefix + index + PROPERTY_KEY_SUFFIX_EXTRACT_SKIP_GLOB);
		String filterGlob = properties.getProperty(prefix + index + PROPERTY_KEY_SUFFIX_EXTRACT_FILTER_GLOB);
		File root = new File(extractRoot);
		Map<String, String> replaceParameters = new HashMap<>();
		for (Entry<Object, Object> next : properties.entrySet()) {
			replaceParameters.put((String)next.getKey(), (String)next.getValue());
		}
		try {
			if (!root.isAbsolute()) {
				root = new File(workDir.getCanonicalFile(), extractRoot).getCanonicalFile();
			}
			if (extractGlob != null || skipExtractGlob != null) {
				extractFile(target, replaceParameters, root, extractGlob, skipExtractGlob, filterGlob, overwrite);
			}
			return true;
		} catch (IOException | CompressorException | ArchiveException e) {
			LogRecord rec = new LogRecord(Level.WARNING, "Failed to extract " + target.getAbsolutePath());
			rec.setThrown(e);
			logger.log(rec);
			return false;
		}
	}
	private void extractFile(File archive, Map<String, String> replaceTokens, File root, 
			String extractGlob, String skipExtractGlob, String filterGlob, boolean overwrite) throws CompressorException, IOException, ArchiveException {
		String lcFileName = archive.getName().toLowerCase();
		Set<PathMatcher> extractMatchers = getGlobMatchers(extractGlob);
		Set<PathMatcher> skipMatchers = getGlobMatchers(skipExtractGlob);
		Set<PathMatcher> filterMatchers = getGlobMatchers(filterGlob);
		if (lcFileName.endsWith(".zip")) {
			try (ZipFile source = new ZipFile(archive)) {
				extractZip(source, root, replaceTokens, extractMatchers, skipMatchers, filterMatchers, overwrite);
			}
		} else {
			try (InputStream in = new BufferedInputStream(new FileInputStream(archive), 8 * 1024)) {
				if (lcFileName.endsWith("z") ||	lcFileName.endsWith("bz2") || lcFileName.endsWith("lzma") ||
						lcFileName.endsWith("arj") || lcFileName.endsWith("deflate")) {
					try (InputStream compressed = cfactory.createCompressorInputStream(in)) {
						try (ArchiveInputStream source = factory.createArchiveInputStream(compressed)) {
							extractArchive(source, root, replaceTokens, extractMatchers, skipMatchers, filterMatchers, overwrite);
						}
					}
				} else {
					try (ArchiveInputStream source = factory.createArchiveInputStream(in)) {
						extractArchive(source, root, replaceTokens, extractMatchers, skipMatchers, filterMatchers, overwrite);
					}
				}
			}
		}
	}
	private void extractZip(ZipFile zipFile, File destFolder,
			Map<String, String> replaceTokens,
			Set<PathMatcher> extractMatchers, Set<PathMatcher> skipMatchers,
			Set<PathMatcher> filterMatchers, boolean overwrite) throws IOException {
		Enumeration<ZipArchiveEntry> en = zipFile.getEntries();
		while (en.hasMoreElements()) {
			ZipArchiveEntry nextEntry = en.nextElement();
			extractEntry(zipFile.getInputStream(nextEntry), (ArchiveEntry) nextEntry, destFolder, replaceTokens, extractMatchers, skipMatchers, filterMatchers, overwrite);
		}
		
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
	private void extractEntry(InputStream is, ArchiveEntry entry, File destFolder, Map<String, String> replaceTokens, Set<PathMatcher> extractMatchers, Set<PathMatcher> skipMatchers, Set<PathMatcher> filterMatchers, boolean overwrite) throws IOException {
		File dest = new File(destFolder, entry.getName()).getCanonicalFile();
		if (globMatches(entry.getName(), extractMatchers) && !globMatches(entry.getName(), skipMatchers)) {
			if (entry.isDirectory()) {
				dest.mkdirs();
			} else {
				if (dest.exists() && !overwrite) return;
				dest.getParentFile().mkdirs();
				if (globMatches(entry.getName(), filterMatchers)) {
					FileUtil.filterStream(is, dest, replaceTokens);
				} else {
					FileUtil.copy(is, dest);
				}
				PosixFileAttributeView posix = Files.getFileAttributeView(dest.toPath(), PosixFileAttributeView.class);
				Set<PosixFilePermission> permissions = getPermissions(getMode(entry));
				if (posix != null) {
					posix.setPermissions(permissions);
				} else {
					for (PosixFilePermission next : permissions) {
						boolean userOnly = false;
						if (next == PosixFilePermission.OWNER_EXECUTE ||
								next == PosixFilePermission.OWNER_READ ||
								next == PosixFilePermission.OWNER_WRITE) {
							userOnly = true;
						}
						if (next == PosixFilePermission.OWNER_EXECUTE ||
								next == PosixFilePermission.GROUP_EXECUTE ||
								next == PosixFilePermission.OTHERS_EXECUTE) {
							dest.setExecutable(true, userOnly);
						}
						if (next == PosixFilePermission.OWNER_WRITE ||
								next == PosixFilePermission.GROUP_WRITE ||
								next == PosixFilePermission.OTHERS_WRITE) {
							dest.setWritable(true, userOnly);
						}
						if (next == PosixFilePermission.OWNER_READ ||
								next == PosixFilePermission.GROUP_READ ||
								next == PosixFilePermission.OTHERS_READ) {
							dest.setReadable(true, userOnly);
						}
					}
				}
			}
		}
	}
	private void extractArchive(ArchiveInputStream is, File destFolder, Map<String, String> replaceTokens, Set<PathMatcher> extractMatchers, Set<PathMatcher> skipMatchers, Set<PathMatcher> filterMatchers, boolean overwrite) throws IOException {
		try {
			ArchiveEntry entry;
			while((entry =  is.getNextEntry()) != null) {
				extractEntry(is, entry, destFolder, replaceTokens, extractMatchers, skipMatchers, filterMatchers, overwrite);
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
				ZipArchiveEntry e = (ZipArchiveEntry)entry; 
				ZipExtraField[] ef = e.getExtraFields(true);
				int ret = (int) ((e.getExternalAttributes() >> 16) & 0xFFF);
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