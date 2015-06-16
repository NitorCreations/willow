package com.nitorcreations.willow.utils;

import java.util.Enumeration;
import java.util.Iterator;

public class EnumerationIterable<T> implements Iterable<T> {
  private final Enumeration<T> enumeration;

  public EnumerationIterable(Enumeration<T> enumeration) {
    this.enumeration = enumeration;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      @Override
      public boolean hasNext() {
        return enumeration.hasMoreElements();
      }
      @Override
      public T next() {
        return enumeration.nextElement();
      }
      @Override
      public void remove() {
        throw new UnsupportedOperationException("Remove not supported in underlying enumeration");
      }
    };
  }
}
