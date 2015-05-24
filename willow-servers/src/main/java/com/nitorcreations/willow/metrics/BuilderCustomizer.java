package com.nitorcreations.willow.metrics;

import org.elasticsearch.action.search.SearchRequestBuilder;

public interface BuilderCustomizer {
  void customize(SearchRequestBuilder builder);
}