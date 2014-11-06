package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;

public abstract class TagListMetric implements Metric<List<String>> {
	
	private final String tagPrefix;
	
	public TagListMetric(String tagPrefix) {
		this.tagPrefix = tagPrefix;
	}
	@Override
	public List<String> calculateMetric(Client client, HttpServletRequest req) {
		long start = Long.parseLong(req.getParameter("start"));
		long stop = Long.parseLong(req.getParameter("stop"));
		String type = req.getParameter("type");
		SearchResponse response = client.prepareSearch(MetricUtils.getIndexes(start, stop, client))
				.setTypes(type)
				.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp")
						.from(start)
						.to(stop)))
						.setSize(0)
				.addAggregation(AggregationBuilders.terms("tags").field("tags").include(tagPrefix + "_.*")).get();
		ArrayList<String> ret = new ArrayList<>();
		StringTerms agg = response.getAggregations().get("tags");
		for (Bucket next : agg.getBuckets()) {
			ret.add(next.getKey());
		}
		return ret;
	}
}
