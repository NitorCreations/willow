package com.nitorcreations.willow.download;

import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_FILTER_GLOB;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_GLOB;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_OVERWRITE;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_ROOT;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_SKIP_GLOB;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_SUFFIX_WRITE_MD5SUMS;
import static com.nitorcreations.willow.properties.PropertyKeys.PROPERTY_KEY_WORKDIR;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import com.nitorcreations.willow.utils.FileUtil;
import com.nitorcreations.willow.utils.MD5SumOutputStream;

@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.AvoidUsingOctalValues"})
public class Extractor implements Callable<Boolean> {
  private final Properties properties;
  private final File target;
  private final Logger logger;
  private static final Pattern zipFilesPattern = Pattern.compile(".*?\\.zip$|.*?\\.jar$|.*?\\.war$|.*?\\.ear$", Pattern.CASE_INSENSITIVE);
  private static final Pattern compressedPattern = Pattern.compile(".*?\\.gz$|.*?\\.tgz$|.*?\\.z$|.*?\\.bz2$|.*?\\.lzma$|.*?\\.arj$|.*?\\.deflate$", Pattern.CASE_INSENSITIVE);
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

  public Extractor(Properties properties, File target) {
    this.properties = properties;
    this.target = target;
    logger = Logger.getLogger(target.getName());
  }

  @Override
  public Boolean call() {
    if (target == null) {
      return false;
    }
    File workDir = new File(properties.getProperty(PROPERTY_KEY_WORKDIR, "."));
    boolean overwrite = "false".equalsIgnoreCase(properties.getProperty(PROPERTY_KEY_SUFFIX_EXTRACT_OVERWRITE)) ? false : true;
    String extractRoot = properties.getProperty(PROPERTY_KEY_SUFFIX_EXTRACT_ROOT, workDir.getAbsolutePath());
    String extractGlob = properties.getProperty(PROPERTY_KEY_SUFFIX_EXTRACT_GLOB);
    String skipExtractGlob = properties.getProperty(PROPERTY_KEY_SUFFIX_EXTRACT_SKIP_GLOB);
    String filterGlob = properties.getProperty(PROPERTY_KEY_SUFFIX_EXTRACT_FILTER_GLOB);
    boolean writeMd5Sums = "true".equalsIgnoreCase(properties.getProperty(PROPERTY_KEY_SUFFIX_WRITE_MD5SUMS, "false"));
    File root = new File(extractRoot);
    Map<String, String> replaceParameters = new HashMap<>();
    for (Entry<Object, Object> next : properties.entrySet()) {
      replaceParameters.put((String) next.getKey(), (String) next.getValue());
    }
    try {
      if (!root.isAbsolute()) {
        root = new File(workDir.getCanonicalFile(), extractRoot).getCanonicalFile();
      }
      int entries = 0;
      if (extractGlob != null || skipExtractGlob != null) {
        entries = extractFile(target, replaceParameters, root, extractGlob, skipExtractGlob, filterGlob, overwrite, writeMd5Sums);
      }
      logger.log(Level.INFO, "Processed " + entries + " entries");
      return true;
    } catch (IOException | CompressorException | ArchiveException e) {
      logger.log(Level.WARNING, "Failed to extract " + target.getAbsolutePath(), e);
      return false;
    }
  }

  private int extractFile(File archive, Map<String, String> replaceTokens, File root, String extractGlob, String skipExtractGlob, String filterGlob, boolean overwrite, boolean writeMd5Sums) throws CompressorException, IOException, ArchiveException {
    Set<PathMatcher> extractMatchers = getGlobMatchers(extractGlob);
    Set<PathMatcher> skipMatchers = getGlobMatchers(skipExtractGlob);
    Set<PathMatcher> filterMatchers = getGlobMatchers(filterGlob);
    if (zipFilesPattern.matcher(archive.getName()).matches()) {
      try (ZipFile source = new ZipFile(archive)) {
        return extractZip(source, root, replaceTokens, extractMatchers, skipMatchers, filterMatchers, overwrite, writeMd5Sums);
      }
    } else {
      try (InputStream in = new BufferedInputStream(new FileInputStream(archive), 8 * 1024)) {
        if (compressedPattern.matcher(archive.getName()).matches()) {
          try (InputStream compressed = new BufferedInputStream(cfactory.createCompressorInputStream(in), 8 * 1024)) {
            try (ArchiveInputStream source = factory.createArchiveInputStream(compressed)) {
              return extractArchive(source, root, replaceTokens, extractMatchers, skipMatchers, filterMatchers, overwrite, writeMd5Sums);
            }
          }
        } else {
          try (ArchiveInputStream source = factory.createArchiveInputStream(in)) {
            return extractArchive(source, root, replaceTokens, extractMatchers, skipMatchers, filterMatchers, overwrite, writeMd5Sums);
          }
        }
      }
    }
  }

  private int extractArchive(ArchiveInputStream is, File destFolder, Map<String, String> replaceTokens, Set<PathMatcher> extractMatchers, Set<PathMatcher> skipMatchers, Set<PathMatcher> filterMatchers, boolean overwrite, boolean writeMd5Sums) throws IOException {
    ArchiveEntry entry;
    int entries = 0;
    while ((entry = is.getNextEntry()) != null) {
      extractEntry(is, entry, destFolder, replaceTokens, extractMatchers, skipMatchers, filterMatchers, overwrite, writeMd5Sums);
      entries++;
    }
    return entries;
  }

  private int extractZip(ZipFile zipFile, File destFolder, Map<String, String> replaceTokens, Set<PathMatcher> extractMatchers, Set<PathMatcher> skipMatchers, Set<PathMatcher> filterMatchers, boolean overwrite, boolean writeMd5Sums) throws IOException {
    Enumeration<ZipArchiveEntry> en = zipFile.getEntries();
    int entries = 0;
    while (en.hasMoreElements()) {
      ZipArchiveEntry nextEntry = en.nextElement();
      extractEntry(zipFile.getInputStream(nextEntry), nextEntry, destFolder, replaceTokens, extractMatchers, skipMatchers, filterMatchers, overwrite, writeMd5Sums);
      entries++;
    }
    return entries;
  }

  private boolean globMatches(String path, Set<PathMatcher> matchers) {
    for (PathMatcher next : matchers) {
      if (next.matches(Paths.get(path))) {
        return true;
      }
    }
    return false;
  }

  private Set<PathMatcher> getGlobMatchers(String expressions) {
    Set<PathMatcher> matchers = new LinkedHashSet<>();
    if (expressions == null || expressions.isEmpty()) {
      return matchers;
    }
    FileSystem def = FileSystems.getDefault();
    for (String next : expressions.split("\\|")) {
      String trimmed = next.trim();
      if (!trimmed.isEmpty()) {
        matchers.add(def.getPathMatcher("glob:" + trimmed));
      }
    }
    return matchers;
  }

  public Set<PosixFilePermission> getPermissions(int mode) {
    Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>();
    for (Entry<Integer, PosixFilePermission> maskEntry : perms.entrySet()) {
      int mask = maskEntry.getKey().intValue();
      if (mask == (mode & mask)) {
        permissions.add(maskEntry.getValue());
      }
    }
    return permissions;
  }
  @SuppressWarnings("PMD.CollapsibleIfStatements")
  private void extractEntry(InputStream is, ArchiveEntry entry, File destFolder, Map<String, String> replaceTokens, Set<PathMatcher> extractMatchers, Set<PathMatcher> skipMatchers, Set<PathMatcher> filterMatchers, boolean overwrite, boolean writeMd5Sums) throws IOException {
    File dest = new File(destFolder, entry.getName()).getCanonicalFile();
    if (globMatches(entry.getName(), extractMatchers) && !globMatches(entry.getName(), skipMatchers)) {
      if (entry.isDirectory()) {
        FileUtil.createDir(dest);
      } else {
        if (dest.exists() && !overwrite) {
          return;
        }
        FileUtil.createDir(dest.getParentFile());
        try (OutputStream out = writeMd5Sums ? new MD5SumOutputStream(new FileOutputStream(dest)) : new FileOutputStream(dest)) {
          if (globMatches(entry.getName(), filterMatchers)) {
            FileUtil.filterStream(is, out, replaceTokens);
          } else {
            FileUtil.copy(is, out);
          }
          if (out instanceof MD5SumOutputStream) {
            byte[] digest = ((MD5SumOutputStream)out).digest();
            File md5file = new File(dest.getParentFile(), dest.getName() + ".md5");
            String md5str = DatatypeConverter.printHexBinary(digest);
            try (OutputStream mdout = new FileOutputStream(md5file)) {
              mdout.write((md5str + "  " + dest.getName() + "\n").getBytes(StandardCharsets.UTF_8));
              mdout.flush();
            }

          }
        }
        PosixFileAttributeView posix = Files.getFileAttributeView(dest.toPath(), PosixFileAttributeView.class);
        Set<PosixFilePermission> permissions = getPermissions(getMode(entry));
        if (posix != null) {
          posix.setPermissions(permissions);
        } else {
          for (PosixFilePermission next : permissions) {
            boolean userOnly = false;
            if (next == PosixFilePermission.OWNER_EXECUTE || next == PosixFilePermission.OWNER_READ || next == PosixFilePermission.OWNER_WRITE) {
              userOnly = true;
            }
            if (next == PosixFilePermission.OWNER_EXECUTE || next == PosixFilePermission.GROUP_EXECUTE || next == PosixFilePermission.OTHERS_EXECUTE) {
              if (dest.setExecutable(true, userOnly)) {
                logger.fine("Failed to set executable on " + dest.getAbsolutePath());
              }
            }
            if (next == PosixFilePermission.OWNER_WRITE || next == PosixFilePermission.GROUP_WRITE || next == PosixFilePermission.OTHERS_WRITE) {
              if (!dest.setWritable(true, userOnly)) {
                logger.fine("Failed to set writable on " + dest.getAbsolutePath());
              }
            }
            if (next == PosixFilePermission.OWNER_READ || next == PosixFilePermission.GROUP_READ || next == PosixFilePermission.OTHERS_READ) {
              if (!dest.setReadable(true, userOnly)) {
                logger.fine("Failed to set readable on " + dest.getAbsolutePath());
              }
            }
          }
        }
      }
    }
  }

  private int getMode(ArchiveEntry entry) {
    Method m = null;
    try {
      m = entry.getClass().getMethod("getMode");
    } catch (NoSuchMethodException | SecurityException e) {
      logger.finer("No mode method on entry " + entry.getName());
    }
    if (m == null) {
      if (entry instanceof ZipArchiveEntry) {
        ZipArchiveEntry e = (ZipArchiveEntry) entry;
        int ret = (int) ((e.getExternalAttributes() >> 16) & 0xFFF);
        if (ret == 0) {
          return 0644;
        } else {
          return ret;
        }
      } else {
        return 0664;
      }
    } else {
      try {
        return (int) ((Number) m.invoke(entry)).longValue() & 0xFFF;
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        return 0664;
      }
    }
  }
}
