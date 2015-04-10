package com.nitorcreations.willow.metrics;

import javax.inject.Named;

@Named("/categories")
public class CategoryTagMetric extends TagListMetric {
  public CategoryTagMetric() {
    super("category");
  }
}
