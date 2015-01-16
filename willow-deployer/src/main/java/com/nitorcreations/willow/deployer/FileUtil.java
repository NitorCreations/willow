package com.nitorcreations.willow.deployer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.nitorcreations.willow.utils.ReplaceTokensInputStream;


public class FileUtil {
	public static final int BUFFER_LEN = 8 * 1024;
	public static synchronized boolean createDir(File dir) {
		return   dir.exists() || dir.mkdirs();
	}
	public static String getFileName(String name) {
		int lastSeparator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		int queryIndex = name.lastIndexOf("?");
		if (queryIndex < 0) queryIndex = name.length();
		return name.substring(lastSeparator + 1, queryIndex);
	}
	public static long copy(InputStream in, File target) throws IOException {
		try (FileOutputStream out = new FileOutputStream(target)) {
			long lenght = copy(in, out);
			out.getFD().sync();
			return lenght;
		}
	}
	public static long copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[BUFFER_LEN];
		long count = 0;
		int n = 0;
		while (-1 != (n = in.read(buffer))) {
			out.write(buffer, 0, n);
			count += n;
		}
		out.flush();
		return count;
	}
	public static long filterStream(InputStream original, File target, Map<String, String> replaceTokens) throws IOException {
		try (FileOutputStream out = new FileOutputStream(target)) {
			return filterStream(original, out, replaceTokens);
		}
	}
	public static long filterStream(InputStream original, FileOutputStream out, Map<String, String> replaceTokens) throws IOException {
		try (InputStream in = new ReplaceTokensInputStream(
				new BufferedInputStream(original, BUFFER_LEN),replaceTokens, 
				ReplaceTokensInputStream.MAVEN_DELIMITERS)) {
			long length = copyByteByByte(in, out);
			return length;
		}
	}
	public static long copyByteByByte(InputStream in, OutputStream out) throws IOException {
		BufferedOutputStream bOut = null;
		if (out instanceof BufferedOutputStream) {
			bOut = (BufferedOutputStream) out;
		} else {
			bOut = new BufferedOutputStream(out, BUFFER_LEN);
		}
		try {
			long i = 0;
			int b;
			while ((b = in.read()) != -1) {
				bOut.write(b);
				i++;
			}
			return i;
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				bOut.flush();
				if (out instanceof FileOutputStream) {
					((FileOutputStream)out).getFD().sync();
				}
			} catch (IOException e0) {
				throw e0;
			} finally {
				try {
					if (in != null) in.close();
				} catch (IOException e1) {
					throw e1;
				} finally {
					if (bOut != null) bOut.close();
				}
			}
		}
	}
}
