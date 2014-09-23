package com.nitorcreations.willow.utils;

import java.util.Enumeration;
import java.util.Iterator;

public class ObjectIteratorEnumertion implements Enumeration<Object> {
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