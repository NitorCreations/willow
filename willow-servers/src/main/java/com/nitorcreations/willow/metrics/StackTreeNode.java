package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.List;

public class StackTreeNode {
  public int value=0;
  public final String key;
  public StackTreeNode(String key) {
    this.key = key;
  }
  public List<StackTreeNode> children = new ArrayList<>();
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    StackTreeNode other = (StackTreeNode) obj;
    if (key == null) {
      if (other.key != null) {
        return false;
      }
    } else if (!key.equals(other.key)) {
      return false;
    }
    return true;
  }
}