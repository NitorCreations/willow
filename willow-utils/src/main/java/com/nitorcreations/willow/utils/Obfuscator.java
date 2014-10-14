package com.nitorcreations.willow.utils;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


public class Obfuscator {

	private static final int SALT_LENGTH = 42;
	private final String propertyKey;
	public Obfuscator() throws IOException {
		String defaultMaster = System.getProperty("user.home") + File.separator + 
				".omaster" + File.separator + ".data" + File.pathSeparator + "md5";
		String masterPwdLocation = System.getProperty("o.datamaster", defaultMaster);
		File masterFile = new File(masterPwdLocation);
		if (!masterFile.exists()) {
			masterFile.getParentFile().mkdirs();
			SecureRandom sr = new SecureRandom();
			byte[] key = new byte[128];
			sr.nextBytes(key);
			try (OutputStream out = new FileOutputStream(masterFile)) {
				out.write(("value=" + printBase64Binary(key) + "\n").getBytes(Charset.forName("UTF-8")));
				out.flush();
			}
			Set<PosixFilePermission> perms = new HashSet<>();
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.OWNER_READ);
			Files.setPosixFilePermissions(masterFile.toPath(), perms);
		}
		this.propertyKey = getFileMaster(masterFile);
	}
	public Obfuscator(File masterFile) throws IOException {
		this(getFileMaster(masterFile));
	}
	public Obfuscator(String key) {
		this.propertyKey = key;
	}
	public String encrypt(String value) {
		return encrypt(propertyKey, value);
	}
	public String decrypt(String ciphertext) {
		return decrypt(propertyKey, ciphertext);
	}
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
		if (args.length == 0) return;
		Obfuscator pwdo = new Obfuscator();
		if (args[0].equals("-d")) {
			for (int i = 1; i < args.length; i++) {
				System.out.println(pwdo.decrypt(args[i]));
			}
		} else {
			for (String next : args) {
				System.out.println(pwdo.encrypt(next));
			}
		}
	}
	public static String encrypt(String skey, String value) {
		byte[] bkey = getKeyBytes(skey);
		Key                     key;
		Cipher                  out;
		CipherOutputStream      cOut;
		ByteArrayOutputStream   bOut;
		key = new SecretKeySpec(bkey, "AES");
		try {
			out = Cipher.getInstance("AES/ECB/PKCS5Padding");
			out.init(Cipher.ENCRYPT_MODE, key);
			byte[] input = value.getBytes(Charset.forName("UTF-8"));
			bOut = new ByteArrayOutputStream();
			cOut = new CipherOutputStream(bOut, out);
			cOut.write(getSalt());
			cOut.write((byte)0xca);
			cOut.write((byte)0xfe);
			cOut.write((byte)0xba);
			cOut.write((byte)0xbe);
			cOut.write(input);
			cOut.flush();
			cOut.close();
			return printBase64Binary(bOut.toByteArray());
		} catch (NoSuchAlgorithmException
				| NoSuchPaddingException | InvalidKeyException 
				| IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static String decrypt(String skey, String encrypted) {
		byte[] bkey = getKeyBytes(skey);
		Key                     key;
		Cipher                  in;
		CipherOutputStream      cIn;
		ByteArrayOutputStream   bIn;
		key = new SecretKeySpec(bkey, "AES");
		try { 
			in = Cipher.getInstance("AES/ECB/PKCS5Padding");
			in.init(Cipher.DECRYPT_MODE, key);
			byte[] input = parseBase64Binary(encrypted);
			bIn = new ByteArrayOutputStream();
			cIn = new CipherOutputStream(bIn, in);
			cIn.write(input);
			cIn.close();
			byte[] result = bIn.toByteArray();
			if (result.length <= (SALT_LENGTH + 4) 
					|| result[SALT_LENGTH] != (byte)0xca
					|| result[SALT_LENGTH + 1] != (byte)0xfe 
					|| result[SALT_LENGTH + 2] != (byte)0xba
					|| result[SALT_LENGTH + 3] != (byte)0xbe) {
				return null;
			}
			return new String(result, SALT_LENGTH + 4, result.length - (SALT_LENGTH + 4), Charset.forName("UTF-8"));
		} catch (NoSuchAlgorithmException
				| NoSuchPaddingException | InvalidKeyException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static byte[] getKeyBytes(String key) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			byte[] raw = key.getBytes(Charset.forName("UTF-8"));
			md.update(raw);
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
	public static String getFileMaster(File masterFile) throws IOException {
		PosixFileAttributeView posix = Files.getFileAttributeView(masterFile.toPath(), PosixFileAttributeView.class);
		if (posix != null) {
			Set<PosixFilePermission> perms = posix.readAttributes().permissions();
			if (perms.contains(PosixFilePermission.GROUP_EXECUTE) ||
					perms.contains(PosixFilePermission.GROUP_WRITE) ||
					perms.contains(PosixFilePermission.GROUP_READ) ||
					perms.contains(PosixFilePermission.OTHERS_EXECUTE) ||
					perms.contains(PosixFilePermission.OTHERS_READ) ||
					perms.contains(PosixFilePermission.OTHERS_WRITE)) {
				throw new IOException("Master file permissions too wide");
			}
		} else {
			AclFileAttributeView aclAw = Files.getFileAttributeView(masterFile.toPath(),
					AclFileAttributeView.class);
			if (aclAw == null) {
				throw new IOException("Unable to read master file permissions. Probably insecure.");
			}
			UserPrincipal owner = aclAw.getOwner();
			List<AclEntry> acl = aclAw.getAcl();
			if (!(acl.size() == 1 && acl.get(0).principal().equals(owner))) {
				throw new IOException("Master file permissions too wide");
			}
		}
		MergeableProperties p = new MergeableProperties();
		try {
			p.load(new FileInputStream(masterFile));
		} catch (IOException e) {}
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {}
		if (p.isEmpty()) {
			byte[] content = Files.readAllBytes(masterFile.toPath());
			md.update(content);
		} else {
			for (Entry<String, String> next : p.backingEntrySet()) {
				md.update(next.getKey().getBytes(Charset.forName("UTF-8")));
				md.update(next.getValue().getBytes(Charset.forName("UTF-8")));
			}
		}
		return printBase64Binary(md.digest());
	}
	private static byte[] getSalt() {
		SecureRandom sr = new SecureRandom();
		byte[] bsalt = new byte[SALT_LENGTH];
		sr.nextBytes(bsalt);
		return bsalt;
	}
}
