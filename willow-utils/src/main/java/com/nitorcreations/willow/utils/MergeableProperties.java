package com.nitorcreations.willow.utils;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;

public class MergeableProperties extends Properties {
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
			Matcher m = PropertyMerge.ARRAY_PROPERTY_REGEX.matcher(k);
			if (m.matches()) {
				String arrKey = m.group(1);
				int i = 0;
				while (table.containsKey(arrKey + "[" + i + "]")) {
					i++;
				}
				arrayIndexes.put(arrKey, Integer.valueOf(i));
				return table.put(arrKey + "[" + i + "]", v);
			} else {
				m = PropertyMerge.ARRAY_REFERENCE_REGEX.matcher(k);
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
	protected MergeableProperties(Properties defaults, LinkedHashMap<String, String> values) {
		this.defaults = defaults;
		table.putAll(values);
	}
	public MergeableProperties() {
		super();
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
		return new MergeableProperties(defaults, table);
    }
	@Override
    public Set<Object> keySet() {
    	return new LinkedHashSet<Object>(table.values());
    }
    public Collection<Object> values() {
    	return new LinkedHashSet<Object>(table.values());
    }
}