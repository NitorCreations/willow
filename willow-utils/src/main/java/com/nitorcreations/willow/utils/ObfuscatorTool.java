package com.nitorcreations.willow.utils;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressWarnings("PMD.EmptyCatchBlock")
public class ObfuscatorTool {
  private static String obfuscatedPrefix="OBF:";
  public static final Pattern ENCRYPTED_TOKEN_PATTERN = Pattern.compile("^([^\\$\\{]*)\\$?\\{([^\\{]*)\\}(.*)$");

  public static void main(String[] args) throws FileNotFoundException, IOException {
    if (args.length == 0) {
      usage("At least one argument expected");
    }
    obfuscatedPrefix = System.getProperty("obfuscatedPrefix", obfuscatedPrefix);
    SecureRandom random = new SecureRandom();
    MergeableProperties in = new MergeableProperties();
    MergeableProperties outProps = new MergeableProperties();
    try (FileInputStream inStream = new FileInputStream(args[0])) {
      in.load(inStream);
    }
    File keyFile = null;
    if (args.length > 1) {
      keyFile = new File(args[1]);
    } else {
      if (System.getProperty("decrypt") != null && args[0].endsWith(".encrypted")) {
        keyFile = new File(args[0].substring(0, args[0].length() - ".encrypted".length()) + ".key");
      } else {
        keyFile = new File(args[0] + ".key");
      }
    }
    if (!keyFile.exists()) {
      try (FileOutputStream out = new FileOutputStream(keyFile)) {
        out.write("#".getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[512];
        random.nextBytes(key);
        out.write(printBase64Binary(key).getBytes(StandardCharsets.UTF_8));
        out.flush();
      }
      if (!keyFile.setExecutable(false, true)) {
        System.out.println("# Failed to set keyfile as not executable");
      }
      if (!keyFile.setExecutable(false, false)) {
        System.out.println("# Failed to set keyfile as not executable");
      }
      if (!keyFile.setReadable(false, true)) {
        System.out.println("# Failed to set keyfile as not readable");
      }
      if (!keyFile.setReadable(false, false)) {
        System.out.println("# Failed to set keyfile as not readable");
      }
      if (!keyFile.setWritable(false, true)) {
        System.out.println("# Failed to set keyfile as not writable");
      }
      if (!keyFile.setWritable(false, false)) {
        System.out.println("# Failed to set keyfile as not readable");
      }
      if (!keyFile.setReadable(true, true)) {
        System.out.println("# Failed to set keyfile as readable by owner");
      }
      if (!keyFile.setWritable(true, true)) {
        System.out.println("# Failed to set keyfile as writable by owner");
      }
    }
    Obfuscator.KeyDigest digest = Obfuscator.KeyDigest.MD5;
    if (System.getProperty("digest") != null) {
      try {
        digest = Obfuscator.KeyDigest.valueOf(System.getProperty("digest"));
      } catch (Throwable t)  {}
    }
    int iterations = 1;
    if (System.getProperty("iterations") != null) {
      try {
        iterations = Integer.parseInt(System.getProperty("iterations"));
      } catch (Throwable t)  {}
    }
    String key = Obfuscator.getFileMaster(keyFile, digest);
    Obfuscator obf = new Obfuscator(key, digest, iterations);
    if (System.getProperty("decrypt") == null) {
      for (Entry<String, String> next : in.backingEntrySet()) {
        String value = next.getValue();
        String prefix = "";
        if (value.startsWith(obfuscatedPrefix)) {
          prefix = obfuscatedPrefix;
          value = value.substring(obfuscatedPrefix.length());
        }
        outProps.put(next.getKey(), prefix + "{" + obf.encrypt(value) + "}");
      }
      try (FileOutputStream out = new FileOutputStream(args[0] + ".encrypted")) {
        outProps.store(out, null);
      }
    } else {
      for (Entry<String, String> next : in.backingEntrySet()) {
        String value = next.getValue();
        String prefix = "";
        if (value.startsWith(obfuscatedPrefix)) {
          prefix = obfuscatedPrefix;
          value = value.substring(obfuscatedPrefix.length());
        }
        Matcher m = ENCRYPTED_TOKEN_PATTERN.matcher(value);
        if (m.matches()) {
          outProps.put(next.getKey(), prefix + m.group(1) + obf.decrypt(m.group(2)) + m.group(3));
        } else {
          outProps.put(next.getKey(), next.getValue());
        }
      }
      outProps.store(System.out, null);
    }
  }
  @SuppressFBWarnings(value={"DM_EXIT"},
      justification="ObfuscatorTool is a command-line utility and thus needs to convey proper exit value")
  private static void usage(String message) {
    System.err.println(message);
    System.err.println("usage: java -jar propertyobfuscator.jar obfuscate.properties [keyfile]");
    System.exit(1);
  }
}
