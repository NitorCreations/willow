package com.nitorcreations.willow.metrics;

import org.elasticsearch.action.search.SearchResponse;

public interface Metric {
	String getType();
	String[] requiresFields();
	Object calculateMetric(SearchResponse resp, long start, long stop, int step);
}