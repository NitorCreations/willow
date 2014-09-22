package com.nitorcreations.willow.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map.Entry;

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

	public static class MergeableProperties extends Properties {
		private static final long serialVersionUID = -2166886363149152785L;
		private final LinkedHashMap<String, String> table = new LinkedHashMap<>();
		private final HashMap<String, Integer> arrayIndexes = new HashMap<>();
		@Override
		public Object put(Object key, Object value) {
			String k = (String)key;
			String v = (String)value;
			String prev = table.get(k);
			if (prev != null && table.get(k + ".appendchar") != null) {
				return table.put(k, prev + table.get(k + ".appendchar") + v);
			} else {
				Matcher m = ARRAY_PROPERTY_REGEX.matcher(k);
				if (m.matches()) {
					String arrKey = m.group(1);
					int i = 0;
					while (table.containsKey(arrKey + "[" + i + "]")) {
						i++;
					}
					arrayIndexes.put(arrKey, Integer.valueOf(i));
					return table.put(arrKey + "[" + i + "]", v);
				} else {
					m = ARRAY_REFERENCE_REGEX.matcher(k);
					if (m.matches()) {
						String arrKey = m.group(1);
						Integer lastIndex = arrayIndexes.get(arrKey);
						if (lastIndex != null) {
							return table.put(arrKey + "[" + lastIndex + "]" + m.group(2), v);
						} else {
							return table.put(k, v);
						}
					} else {
						return table.put(k, v);
					}
				}
			}
		}
		@Override
		public Enumeration<Object> keys() {
			return new ObjectIteratorEnumertion(table.keySet().iterator());
		}
		@Override
		public Object get(Object key) {
			return table.get(key);
		}
		@Override
		public String getProperty(String key) {
			String oval = table.get(key);
			return ((oval == null) && (defaults != null)) ? defaults.getProperty(key) : oval;
		}
		public void putAll(MergeableProperties toMerge) {
			for (Entry<String, String> next: toMerge.table.entrySet()) {
				put(next.getKey(), next.getValue());
			}
		}
		public Set<Entry<String, String>> backingEntrySet() {
			return table.entrySet();
		}
		public Map<String, String> backingTable() {
			return table;
		}
		@Override
		public Object remove(Object key) {
			return table.remove((String)key);
		}
		@Override
		public String toString() {
			return table.toString();
		}
	}
	public static class ObjectIteratorEnumertion implements Enumeration<Object> {
		private final Iterator<String> it;
		public ObjectIteratorEnumertion(Iterator<String> it) {
			this.it = it;
		}
		@Override
		public boolean hasMoreElements() {
			return it.hasNext();
		}
		@Override
		public Object nextElement() {
			return it.next();
		}
	}
}