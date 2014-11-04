package com.nitorcreations.willow.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.sort.SortOrder;

public class DiskStatusMetric implements Metric<Map<String, long[]>>{

	@Override
	public Map<String, long[]> calculateMetric(Client client,
			HttpServletRequest req) {
		long stop = Long.parseLong(req.getParameter("stop"));
		long start = stop - TimeUnit.DAYS.toMillis(1);
		String[] tags = req.getParameterValues("tag");
		SearchRequestBuilder builder = client.prepareSearch(MetricUtils.getIndexes(start, stop))
				.setTypes("disk")
				.addField("timestamp")
				.addField("name")
				.addField("total")
				.addField("free")
				.setSize(50).addSort("timestamp", SortOrder.DESC);
		BoolQueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp")
				.from(start)
				.to(stop));
		for (String tag : tags) {
	    	query = query.must(QueryBuilders.termQuery("tags", tag));
		}
		SearchResponse response = builder.setQuery(query).get();
		Map<String, long[]> ret = new HashMap<>();
		for (SearchHit next : response.getHits().getHits()) {
			Map<String, SearchHitField> results =  next.getFields();
			String name = (String)results.get("name").getValue();
			long[] retRes = new long[2];
			retRes[0] = ((Number)results.get("total").value()).longValue();
			if (retRes[0] == 0) continue;
			retRes[1] = ((Number)results.get("free").value()).longValue();
			if (ret.containsKey(name)) break;
			ret.put(name, retRes);
		}
		return ret;
	}
}
