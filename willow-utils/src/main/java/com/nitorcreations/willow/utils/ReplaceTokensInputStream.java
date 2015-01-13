package com.nitorcreations.willow.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplaceTokensInputStream extends FilterInputStream {

	public static class StartEndDelimeters {
		public final String start;
		public final String end;
		public StartEndDelimeters(String start, String end) {
			this.start = start;
			this.end = end;
		}
	}
	private final StartEndDelimeters[] delimiters;
	private final Map<String, String> tokens;
	private final Charset charset;
	private int shortestTokenLength = Integer.MAX_VALUE;
	private int longestTokenLength = 0;
	private int shortestDelimeterLength = Integer.MAX_VALUE;
	private int longestDelimeterLength = 0;
	private boolean streamExhausted = false;
	private List<Integer> available = new ArrayList<>();
	private int end = 0;
	private final byte[] buffer;
	public static final StartEndDelimeters AT_DELIMITERS = new StartEndDelimeters("@", "@");
	public static final StartEndDelimeters CURLY_DELIMITERS = new StartEndDelimeters("${", "}");
	public static final StartEndDelimeters[] MAVEN_DELIMITERS = new StartEndDelimeters[] { AT_DELIMITERS, CURLY_DELIMITERS }; 

	public ReplaceTokensInputStream(InputStream in, Map<String, String> tokens, StartEndDelimeters ... delimiters) {
		this(in, Charset.defaultCharset(), tokens, delimiters);
	}
	public ReplaceTokensInputStream(InputStream in, Charset charset, Map<String, String> tokens, StartEndDelimeters ... delimiters) {
		super(in);
		this.delimiters = delimiters;
		this.tokens = tokens;
		this.charset = charset;
		for (String next : tokens.keySet()) {
			int nextLen = next.getBytes(charset).length;
			if (nextLen < shortestTokenLength) {
				shortestTokenLength = nextLen;
			}
			if (nextLen > longestTokenLength) {
				longestTokenLength = nextLen;
			}
		}
		for (StartEndDelimeters next : delimiters) {
			int nextLen = (next.start + next.end).getBytes(charset).length;
			if (nextLen < shortestDelimeterLength) {
				shortestDelimeterLength = nextLen;
			}
			if (nextLen > longestDelimeterLength) {
				longestDelimeterLength = nextLen;
			}
		}
		buffer = new byte[longestDelimeterLength + longestTokenLength];
	}
	@Override
	public int read() throws IOException {
		if (!available.isEmpty()) {
			return available.remove(0);
		}
		if (!streamExhausted) {
			append(superRead());
		} else if (end < 1) {
			return -1;
		} else {
			shift(end, true);
			return available.remove(0);
		}
		String bufferStr = getBufferString();
		List<StartEndDelimeters> potentialMatches = getPotentialMatches(bufferStr);
		if (potentialMatches.isEmpty()) {
			shift(1, true);
			return available.remove(0);
		}
		String matchToken = null;
		do {
			matchToken = getMatchToken(bufferStr, potentialMatches);
			if (potentialMatches.isEmpty()) break;
			if (matchToken != null) {
				for (byte next : matchToken.getBytes(charset)) {
					available.add((int)next & 0xFF);
				}
				return available.remove(0);
			}
			if (end == buffer.length) break;
			append(superRead());
			bufferStr = getBufferString();
		} while (matchToken == null && !streamExhausted);
		if (end < 1 && available.isEmpty()) return -1;
		shift(1, true);
		return available.remove(0);
	}
	private void append(int next) {
		if (next != -1) {
			buffer[end] = (byte)(next & 0xFF);
			++end;
		}
	}
	private void shift(int shift, boolean makeAvailable) {
		for (int i=shift; i<=end;i++) {
			if (makeAvailable) {
				available.add((int)buffer[i-shift] & 0xFF);
			}
			if (i==end) break;
			buffer[i-shift] = buffer[i];
		}
		end-=shift;
	}
	private String getMatchToken(String bufferStr, List<StartEndDelimeters> potentialMatches) {
		for (StartEndDelimeters next : potentialMatches.toArray(new StartEndDelimeters[potentialMatches.size()])) {
			int nextMinLength = next.start.length() + next.end.length() + 1;
			if (bufferStr.length() >= nextMinLength && bufferStr.startsWith(next.start)) {
				int endIndex = bufferStr.indexOf(next.end, next.start.length() + 1);
				if (endIndex > 0) {
					String ret = tokens.get(bufferStr.substring(next.start.length(), endIndex));
					if (ret != null) {
						shift(endIndex + next.end.length(), false);
						return ret;
					}
				}
			} else if (bufferStr.length() >= next.start.length() && !bufferStr.startsWith(next.start)) {
				potentialMatches.remove(next);
			}
		}
		return null;
	}
	private int superRead() throws IOException {
		int ret = super.read();
		if (ret == -1) {
			streamExhausted = true;
		}
		return ret;
	}
	private List<StartEndDelimeters> getPotentialMatches(String bufferStr) {
		List<StartEndDelimeters> ret = new ArrayList<>();
		for (StartEndDelimeters next : delimiters) {
			int len = Math.min(next.start.length(), bufferStr.length());
			if (next.start.substring(0, len).equals(bufferStr.subSequence(0, len))) {
				ret.add(next);
			}
		}
		return ret;
	}
	private String getBufferString() {
		return new String(buffer, 0, end, charset);
	}
}
