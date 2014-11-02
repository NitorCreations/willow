package com.nitorcreations.willow.metrics;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.client.Client;

public interface Metric<T> {
	T calculateMetric(Client client, HttpServletRequest req);
}