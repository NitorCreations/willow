package com.nitorcreations.willow.metrics;

import java.util.LinkedHashMap;
import java.util.Map;


public class StackTreeNode {
  public int count;
  public Map<String, StackTreeNode> children = new LinkedHashMap<>();
}