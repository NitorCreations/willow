package com.nitorcreations.willow.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.StrSubstitutor;

public class MergeableProperties extends Properties {
	public static final Pattern ARRAY_PROPERTY_REGEX = Pattern.compile("(.*?)\\[\\d*?\\](\\}?)$");
	public static final Pattern ARRAY_REFERENCE_REGEX = Pattern.compile("(\\$\\{)?(.*?)\\[last\\](.*)$");
	public static final String URL_PREFIX_CLASSPATH = "classpath:";
	public static final String INCLUDE_PROPERTY = "include.properties";
	private Logger log = Logger.getLogger(getClass().getName());
	private final String[] prefixes;
	private static final long serialVersionUID = -2166886363149152785L;
	private LinkedHashMap<String, String> table = new LinkedHashMap<>();
	private final HashMap<String, Integer> arrayIndexes = new HashMap<>();
	
	protected MergeableProperties(Properties defaults, LinkedHashMap<String, String> values, String ... prefixes) {
		super(defaults);
		table.putAll(values);
		this.prefixes = prefixes;
	}
	public MergeableProperties() {
		super();
		prefixes = new String[] { "classpath:" };
	}
	public MergeableProperties(String ... prefixes) {
		super();
		this.prefixes = prefixes;
	}
	public Properties merge(String name) {
		merge0(name);
		return this;
	}
	public Properties merge(Properties prev, String name) {
		if (prev != null) {
			putAll(prev);
		}
		merge0(name);
		postMerge();
		return this;
	}
	private void postMerge() {
		LinkedHashMap<String, String> finalTable = new LinkedHashMap<>();
		StrSubstitutor sub = new StrSubstitutor(table, "${", "}", '\\');
		for (Entry<String, String> next : table.entrySet()) {
			finalTable.put(sub.replace(next.getKey()), sub.replace(next.getValue()));
		}
		table = finalTable;
	}
	public void deObfuscate(PropertySource source, String obfuscatedPrefix) {
		if (obfuscatedPrefix == null) return;
		LinkedHashMap<String, String> finalTable = new LinkedHashMap<>();
		for (Entry<String, String> next : table.entrySet()) {
			String value = next.getValue();
			if (value.startsWith(obfuscatedPrefix)) {
				value = source.getProperty(value.substring(obfuscatedPrefix.length()));
			}
			finalTable.put(next.getKey(), value);
		}
		table = finalTable;
	}
	private void merge0(String name) {
		put(INCLUDE_PROPERTY + ".appendchar", "|");
		for (String nextPrefix : prefixes) {
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
					//Logged in the next if else block
				}
			}
			if (in != null) {
				try {
					load(in);
				} catch (IOException e) {
					LogRecord rec = new LogRecord(Level.INFO, "Failed to render url: " + url);
					this.log.log(rec);
				}
			} else {
				log.info("Failed to load url: " + url);
			}
		}
		String include = (String) remove(INCLUDE_PROPERTY);
		if (include != null && !include.isEmpty()) {
			for (String nextInclude : include.split("\\|")) {
				if (log.isLoggable(Level.FINE)) {
					log.fine("Including file: " + nextInclude);
				}
				merge0(nextInclude);
			}
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	public Set<Entry<Object, Object>> entrySet() {
		@SuppressWarnings("rawtypes")
		Set ret = table.entrySet();
		return (Set<Entry<Object, Object>>)ret; 
	}
	@Override
	public Object put(Object key, Object value) {
		String k = resolveIndexes((String)key);
		String v = resolveIndexes((String)value);
		StrSubstitutor sub = new StrSubstitutor(table, "@", "@", '\\');
		k = sub.replace(k);
		v = sub.replace(v);
		String prev = table.get(k);
		if (prev != null && table.get(k + ".appendchar") != null) {
			return table.put(k, prev + table.get(k + ".appendchar") + v);
		} else {
			return table.put(k, v);
		}
	}
	protected String resolveIndexes(String original) {
		String ret = original;
		Matcher m = ARRAY_REFERENCE_REGEX.matcher(ret);
		while (m.matches()) {
			String arrKey = m.group(2);
			Integer lastIndex = arrayIndexes.get(arrKey);
			String prefix = "";
			if (m.group(1) != null) {
				prefix = m.group(1);
			}
			if (lastIndex != null) {
				ret = prefix + arrKey + "[" + lastIndex + "]" + m.group(3);
				m = ARRAY_REFERENCE_REGEX.matcher(ret);
			} else {
				break;
			}
		}
		m = ARRAY_PROPERTY_REGEX.matcher(ret);
		if (m.matches()) {
			String arrKey = m.group(1);
			int i = 0;
			if (arrayIndexes.get(arrKey) != null) {
				i = arrayIndexes.get(arrKey).intValue() + 1;
			}
			while (table.containsKey(arrKey + "[" + i + "]")) {
				i++;
			}
			arrayIndexes.put(arrKey, Integer.valueOf(i));
			ret = arrKey + "[" + i + "]";
			if (m.group(2) != null) {
				ret = ret + m.group(2);
			}
		}
		return ret;
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
	public List<String> getArrayProperty(String key) {
		int i=0;
		ArrayList<String> ret = new ArrayList<>();
		String next = getProperty(key + "[" + i + "]");
		while (next != null) {
			ret.add(next);
			next = getProperty(key + "[" + ++i  + "]");
		}
		return ret;
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
	@Override
	public Enumeration<?> propertyNames() {
		return new ObjectIteratorEnumertion(table.keySet().iterator());
	}
	@Override
	public Set<String> stringPropertyNames() {
		return table.keySet();
	}
	@Override
	public synchronized int size() {
		return table.size();
	}
	@Override
	public synchronized boolean isEmpty() {
		return table.isEmpty() && defaults.isEmpty();
	}
	@Override
	public synchronized Enumeration<Object> elements() {
		return null;
	}
	@Override
	public synchronized boolean contains(Object value) {
		return table.containsValue(value) || defaults.containsValue(value);
	}
	@Override
	public synchronized boolean containsKey(Object key) {
		return table.containsKey(key) || defaults.containsKey(key);
	}
	@Override
	public synchronized Object clone() {
		return new MergeableProperties(defaults, table, prefixes);
	}
	@Override
	public Set<Object> keySet() {
		return new LinkedHashSet<Object>(table.values());
	}
	public Collection<Object> values() {
		return new LinkedHashSet<Object>(table.values());
	}
}