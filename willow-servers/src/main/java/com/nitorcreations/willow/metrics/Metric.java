package com.nitorcreations.willow.metrics;

import java.util.List;

import org.elasticsearch.action.search.SearchResponse;

public interface Metric {
	String getType();
	String[] requiresFields();
	Object calculateMetric(List<SearchResponse> response, long start, long stop, int step);
}