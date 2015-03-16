package com.nitorcreations.willow.utils;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObfuscatorTool {
  private static String obfuscatedPrefix="OBF:";
  public static final Pattern ENCRYPTED_TOKEN_PATTERN = Pattern.compile("^\\$?\\{(.+)?\\}.*$");

  public static void main(String[] args) throws FileNotFoundException, IOException {
    if (args.length == 0) usage("At least one argument expected");
    obfuscatedPrefix = System.getProperty("obfuscatedPrefix", obfuscatedPrefix);
    SecureRandom random = new SecureRandom();
    MergeableProperties in = new MergeableProperties();
    MergeableProperties outProps = new MergeableProperties();
    in.load(new FileInputStream(args[0]));
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
        out.write("#".getBytes());
        byte[] key = new byte[512];
        random.nextBytes(key);
        out.write(printBase64Binary(key).getBytes());
        out.flush();
      }
      keyFile.setExecutable(false, true);
      keyFile.setExecutable(false, false);
      keyFile.setReadable(false, true);
      keyFile.setReadable(false, false);
      keyFile.setWritable(false, true);
      keyFile.setWritable(false, false);
      keyFile.setReadable(true, true);
      keyFile.setWritable(true, true);
    }
    Obfuscator obf = new Obfuscator(keyFile);
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
          outProps.put(next.getKey(), prefix + obf.decrypt(m.group(1)));
        } else {
          outProps.put(next.getKey(), next.getValue());
        }
      }
      outProps.store(System.out, null);
    }
  }
  private static void usage(String message) {
    System.err.println(message);
    System.err.println("usage: java -jar propertyobfuscator.jar obfuscate.properties [keyfile]");
    System.exit(1);
  }

}
