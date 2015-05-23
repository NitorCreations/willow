package com.nitorcreations.willow.metrics;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Named;

import com.nitorcreations.willow.messages.StackTraceData;
import com.nitorcreations.willow.messages.ThreadData;
import com.nitorcreations.willow.messages.ThreadDumpMessage;
import com.nitorcreations.willow.messages.metrics.MetricConfig;

@Named("/stacktrace")
public class StackTraceMetric extends FullMessageMetric<ThreadDumpMessage, StackTreeNode> {
  @Override
  protected StackTreeNode processData(long start, long stop, int step, MetricConfig conf) {
    StackTreeNode root = new StackTreeNode("root");
    Iterator<Entry<Long, ThreadDumpMessage>> it = rawData.entrySet().iterator();
    while (it.hasNext()) {
      ThreadDumpMessage next = it.next().getValue();
      it.remove();
      for (ThreadData nextData : next.threadData) {
        root.value++;
        List<StackTreeNode> nextRoot = root.children;
        for (int i = nextData.stackTrace.length - 1; i > -1; i--) {
          StackTraceData nextStack = nextData.stackTrace[i];
          String key = nextStack.declaringClass + ":" + nextStack.lineNumber;
          StackTreeNode node = new StackTreeNode(key);
          int index = nextRoot.indexOf(node);
          if (index == -1) {
            nextRoot.add(node);
          } else {
            node = nextRoot.get(index);
          }
          node.value++;
          nextRoot = node.children;
        }
      }
    }
    return root;
  }
}
