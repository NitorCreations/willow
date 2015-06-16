package com.nitorcreations.willow.metrics;

import org.elasticsearch.action.search.SearchRequestBuilder;

public class OneResultBuilderCustomizer implements BuilderCustomizer {
  @Override
  public void customize(SearchRequestBuilder builder) {
    builder.setSize(1);
  }
}
