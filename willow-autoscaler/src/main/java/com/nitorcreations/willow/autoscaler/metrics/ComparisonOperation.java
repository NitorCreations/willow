package com.nitorcreations.willow.autoscaler.metrics;

import java.util.HashMap;
import java.util.Map;

public enum ComparisonOperation {
  LESS_THAN("<") {
    @Override
    boolean compare(Double actual, Double threshold) {
      return actual.compareTo(threshold) < 0;
    }
  },
  GREATER_THAN(">") {
    @Override
    boolean compare(Double actual, Double threshold) {
      return actual.compareTo(threshold) > 0;
    }
  },
  EQUAL("=") {
    @Override
    boolean compare(Double actual, Double threshold) {
      return actual.compareTo(threshold) == 0;
    }
  };

  private static Map<String, ComparisonOperation> bySymbol = new HashMap<>();
  static {
    for (ComparisonOperation op : ComparisonOperation.values()) {
      bySymbol.put(op.getSymbol(), op);
    }
  }

  private final String symbol;
  private ComparisonOperation(String symbol) {
    this.symbol = symbol;
  }

  abstract boolean compare(Double actual, Double threshold);

  String getSymbol() {
    return this.symbol;
  }

  static ComparisonOperation fromSymbol(String symbol) {
    ComparisonOperation op = bySymbol.get(symbol);
    if (op == null) {
      throw new IllegalArgumentException("Unknown comparison operation: " + symbol);
    }
    return op;
  }
}
