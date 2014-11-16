package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.sort.SortOrder;

import com.nitorcreations.willow.messages.MessageMapping;

public class MessageTypesMetric implements Metric<Set<String>> {
	@Override
	public Set<String> calculateMetric(Client client, HttpServletRequest req) {
		long start = Long.parseLong(req.getParameter("start"));
		long stop = Long.parseLong(req.getParameter("stop"));
		String[] tags = req.getParameterValues("tag");
		BoolQueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp")
				.from(start)
				.to(stop));
		for (String tag : tags) {
	    	query = query.must(QueryBuilders.termQuery("tags", tag));
		}
		SearchResponse response = client.prepareSearch(MetricUtils.getIndexes(start, stop, client))
				.setTypes(MessageMapping.MessageType.lcNames())
                .setSearchType(SearchType.QUERY_AND_FETCH)
				.setQuery(query)
				.setSize((int)(stop - start)/10)
				.addFields("timestamp", "childName")
				.addSort("timestamp", SortOrder.DESC)
			    .get();
		Set<String> ret = new HashSet<>();
		for (SearchHit next : response.getHits().getHits()) {
			if (next.getFields().get("childName") != null) {
				ret.add(next.getType() + ":" + next.getFields().get("childName").getValue());
			} else {
				ret.add(next.getType());
			}
		}
		return ret;
	}
}
