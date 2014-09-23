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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;

public class MergeableProperties extends Properties {
	private static final long serialVersionUID = -2166886363149152785L;
	private final LinkedHashMap<String, String> table = new LinkedHashMap<>();
	private final HashMap<String, Integer> arrayIndexes = new HashMap<>();
	private BiConsumer<? super String, ? super String> action;
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
    @Override
    public synchronized void forEach(BiConsumer<? super Object, ? super Object> action) {
		table.forEach(action);
    }
    @SuppressWarnings("unchecked")
	@Override
    public synchronized void replaceAll(BiFunction<? super Object, ? super Object, ? extends Object> function) {
		table.replaceAll((BiFunction<? super String, ? super String, ? extends String>) function);
    }
    @Override
    public synchronized String putIfAbsent(Object key, Object value) {
    	return table.putIfAbsent((String)key, (String)value);
    }
    @Override
    public synchronized boolean remove(Object key, Object value) {
    	return table.remove(key, value);
    }
    @Override
    public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
    	return table.replace((String)key, (String)oldValue, (String)newValue);
    }
    @Override
    public synchronized Object replace(Object key, Object value) {
    	return table.replace((String)key, (String)value);
    }
    @SuppressWarnings("unchecked")
	@Override
    public synchronized Object computeIfAbsent(Object key, Function<? super Object, ? extends Object> mappingFunction) {
    	return table.computeIfAbsent((String)key, (Function<? super String, ? extends String>)mappingFunction);
    }
    @SuppressWarnings("unchecked")
	@Override
    public synchronized Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
    	return table.computeIfPresent((String)key, (BiFunction<? super String, ? super String, ? extends String>)remappingFunction);
    }
    @SuppressWarnings("unchecked")
	@Override
    public synchronized Object compute(Object key, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
    	return table.compute((String)key, (BiFunction<? super String, ? super String,? extends String>)remappingFunction);
    	
    }
    @SuppressWarnings("unchecked")
	@Override
    public synchronized Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
    	return table.computeIfAbsent((String)key, (Function<? super String, ? extends String>)remappingFunction);
    	
    }
}