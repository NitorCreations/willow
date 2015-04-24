package com.nitorcreations.willow.metrics;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;

import com.nitorcreations.willow.messages.StackTraceData;
import com.nitorcreations.willow.messages.ThreadData;
import com.nitorcreations.willow.messages.ThreadDumpMessage;

@Named("/stacktrace")
public class StackTraceMetric extends FullMessageMetric<ThreadDumpMessage, Map<String, StackTreeNode>> {
  @Override
  protected Map<String, StackTreeNode> processData(long start, long stop, int step) {
    Map<String, StackTreeNode> root = new LinkedHashMap<>();
    Iterator<Entry<Long, ThreadDumpMessage>> it = rawData.entrySet().iterator();
    while (it.hasNext()) {
      ThreadDumpMessage next = it.next().getValue();
      it.remove();
      for (ThreadData nextData : next.threadData) {
        Map<String, StackTreeNode> nextRoot = root;
        for (int i = nextData.stackTrace.length - 1; i > -1; i--) {
          StackTraceData nextStack = nextData.stackTrace[i];
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
