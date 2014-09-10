package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;

public class TagsList extends AbstractMetric {

	@Override
	public Object calculateMetric(Client client, HttpServletRequest req) {
		long start = Long.parseLong(req.getParameter("start"));
		long stop = Long.parseLong(req.getParameter("stop"));
		
		SearchRequestBuilder request = client.prepareSearch(getIndexes(start, stop))
				.setQuery(QueryBuilders.rangeQuery("timestamp")
						.from(start)
						.to(stop )
						.includeLower(true)
						.includeUpper(true))
				.addAggregation(AggregationBuilders.terms("tags").field("tags"))
				.setSearchType(SearchType.COUNT);
		SearchResponse response = request.get();
		Terms tags = response.getAggregations().get("tags");
		List<String> ret = new ArrayList<>();
		for (Bucket next : tags.getBuckets()) {
			ret.add(next.getKey());
		}
		return ret;
	}

}
