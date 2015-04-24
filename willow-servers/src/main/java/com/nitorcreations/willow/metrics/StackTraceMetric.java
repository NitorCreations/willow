package com.nitorcreations.willow.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Named;

import com.nitorcreations.willow.messages.StackTraceData;
import com.nitorcreations.willow.messages.ThreadData;
import com.nitorcreations.willow.messages.ThreadDumpMessage;

@Named("/stacktrace")
public class StackTraceMetric extends FullMessageMetric<ThreadDumpMessage, Map<String, StackTreeNode>> {
  @Override
  protected Map<String, StackTreeNode> processData(long start, long stop, int step) {
    Map<String, StackTreeNode> root = new LinkedHashMap<>();
    for (ThreadDumpMessage next : rawData.values()) {
      for (ThreadData nextData : next.threadData) {
        Map<String, StackTreeNode> nextRoot = root;
        for (StackTraceData nextStack : nextData.stackTrace) {
          String key = nextStack.declaringClass + ":" + nextStack.lineNumber;
          StackTreeNode node = nextRoot.get(key);
          if (node == null) {
            node = new StackTreeNode();
            node.count = 0;
            nextRoot.put(key, node);
          }
          node.count++;
          nextRoot = node.children;
        }
      }
    }
    return root;
  }
}
