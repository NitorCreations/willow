package com.nitorcreations.willow.metrics;

import org.elasticsearch.action.search.SearchResponse;

public interface HistogramMetric extends Metric {
	Object calculateHistogram(SearchResponse response, double[] buckets, long start, long stop, int step);
}