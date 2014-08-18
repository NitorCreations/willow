package com.nitorcreations.willow.metrics;

import org.elasticsearch.action.search.SearchResponse;

public interface Metric {
	String getIndex();
	String[] requiresFields();
	Object calculateMetric(SearchResponse response, long start, long stop, int step);
}