package com.nitorcreations.willow.metrics;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.client.Client;

public interface Metric {
	Object calculateMetric(Client client, HttpServletRequest req);
}