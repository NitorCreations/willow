package com.nitorcreations.willow.metrics;

import java.util.List;

import org.elasticsearch.action.search.SearchResponse;

public interface HistogramMetric extends Metric {
	Object calculateHistogram(SearchResponse resp, double[] buckets, long start, long stop, int step);
}