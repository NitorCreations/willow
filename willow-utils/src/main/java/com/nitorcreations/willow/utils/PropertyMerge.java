package com.nitorcreations.willow.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.codehaus.swizzle.stream.ReplaceVariablesInputStream;

public class PropertyMerge {
	Logger log = Logger.getLogger(getClass().getName());
	public static final Pattern ARRAY_PROPERTY_REGEX = Pattern.compile("(.*?)\\[\\d*?\\]$");
	public static final Pattern ARRAY_REFERENCE_REGEX = Pattern.compile("(.*?)\\[last\\](.*)$");
	public static final String URL_PREFIX_CLASSPATH = "classpath:";
	public static final String INCLUDE_PROPERTY = "include.properties";
	private final String[] prefixes;
	Properties p = new Properties();
	public PropertyMerge() {
		this("classpath:");
	}
	public PropertyMerge(String ... prefixes) {
		this.prefixes = prefixes;
	}

	public Properties merge(String name) {
		return merge(null, name);
	}
	public Properties merge(Properties prev, String name) {
		MergeableProperties ret = new MergeableProperties();
		ret.putAll(prev);
		return merge(ret, name);
	}

	private MergeableProperties merge(MergeableProperties prev, String name) {
		if (prev == null) prev = new MergeableProperties();
		prev.put(INCLUDE_PROPERTY + ".appendchar", "|");
		for (String nextPrefix : prefixes) {
			Map<String, String> tokens = prev.backingTable();
			String url = nextPrefix + name;
			InputStream in = null;
			if (url.startsWith(URL_PREFIX_CLASSPATH)) {
				in = getClass().getClassLoader().getResourceAsStream(url.substring(URL_PREFIX_CLASSPATH.length()));
			} else {
				try {
					URL toFetch = new URL(url);
					URLConnection conn = toFetch.openConnection(); 
					conn.connect();
					in = conn.getInputStream();
				} catch (IOException e) {
					LogRecord rec = new LogRecord(Level.INFO, "Failed to load url: " + url);
					rec.setThrown(e);
					this.log.log(rec);
				}
			}
			if (in != null) {
				try {
					prev.load(new ReplaceVariablesInputStream(in, "${", "}", tokens));
				} catch (IOException e) {
					LogRecord rec = new LogRecord(Level.INFO, "Failed to render url: " + url);
					rec.setThrown(e);
					this.log.log(rec);
				}
			}
		}
		String include = (String) prev.remove(INCLUDE_PROPERTY);
		if (include != null && !include.isEmpty()) {
			for (String nextInclude : include.split("\\|")) {
				prev = merge(prev, nextInclude);
			}
		}
		return prev;
	}
}