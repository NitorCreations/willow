package com.nitorcreations.willow.metrics;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;

public class HostsMetric extends AbstractMetric {

	@Override
	public Object calculateMetric(Client client, HttpServletRequest req) {
		long start = Long.parseLong(req.getParameter("start"));
		long stop = Long.parseLong(req.getParameter("stop"));
		String type = req.getParameter("type");
		SearchResponse response = client.prepareSearch(getIndexes(start, stop))
				.setTypes(type)
				.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp")
						.from(start)
						.to(stop)))
						.setSize(0)
				.addAggregation(AggregationBuilders.terms("tags").field("tags").include("host_.*")).get();
		ArrayList<String> ret = new ArrayList<>();
		StringTerms agg = response.getAggregations().get("tags");
		for (Bucket next : agg.getBuckets()) {
			ret.add(next.getKey());
		}
		return ret;
	}
}