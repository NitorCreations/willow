package com.nitorcreations.willow.servers;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IterableNodeList implements Iterable<Node> {
  private NodeList list;
  private int index = 0;

  public IterableNodeList(NodeList list) {
    this.list = list;
  }
  public int length() {
    return list.getLength();
  }
  public void skipNext() {
    synchronized (list) {
      index++;
    }
  }
  public void skip(int nodes) {
    synchronized (list) {
      index += nodes;
    }
  }
  public Node item(int index) {
    synchronized (list) {
      return list.item(index);
    }               
  }
  public int getLength() {
    synchronized (list) {
      return list.getLength();
    }               
  }
  public int getIndex() {
    synchronized (list) {
      return (index - 1);
    }               
  }
  @Override
  public Iterator<Node> iterator() {
    return new Iterator<Node>() {
      @Override
      public boolean hasNext() {
        synchronized (list) {
          return index < list.getLength();
        }
      }
      @Override
      public Node next() {
        synchronized (list) {
          if (index >= list.getLength())
            throw new NoSuchElementException();
          return list.item(index++);
        }
      }
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
