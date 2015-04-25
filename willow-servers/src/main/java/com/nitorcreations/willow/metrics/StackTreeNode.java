package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.List;

public class StackTreeNode {
  public int value=0;
  public final String name;
  public StackTreeNode(String name) {
    this.name = name;
  }
  public List<StackTreeNode> children = new ArrayList<>();
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    StackTreeNode other = (StackTreeNode) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }
}