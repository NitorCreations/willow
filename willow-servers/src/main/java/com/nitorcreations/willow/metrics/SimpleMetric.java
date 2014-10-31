package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

public abstract class  SimpleMetric<L> extends AbstractMetric implements Metric {
	protected SortedMap<Long, L> rawData;
	public abstract String getType();
	public abstract String[] requiresFields();

	protected void readResponse(SearchResponse response) {
		rawData = new TreeMap<Long, L>();
		for (SearchHit next : response.getHits().getHits()) {
			Map<String, SearchHitField> fields = next.getFields();
			Long timestamp = fields.get("timestamp").value();
			List<Number> nextData = new ArrayList<>();
			for (String nextField : requiresFields()) {
				nextData.add((Number)fields.get(nextField).value());
			}
			rawData.put(timestamp, getValue(nextData));
		}
	}

	protected L getValue(List<Number> arr) {
		return (L) arr.get(0);
	}
	
	@Override
	public Object calculateMetric(Client client, HttpServletRequest req) {
		long start = Long.parseLong(req.getParameter("start"));
		long stop = Long.parseLong(req.getParameter("stop"));
		int step = Integer.parseInt(req.getParameter("step"));
		String tag = req.getParameter("tag");
		SearchResponse response = client.prepareSearch(getIndexes(start, stop))
				.setTypes(getType())
				.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp")
						.from(start - step)
						.to(stop + step)
						.includeLower(false)
						.includeUpper(true)).must(QueryBuilders.termQuery("tags", tag)))
						.setSearchType(SearchType.QUERY_AND_FETCH)
						.setSize(50000)
						.addField("timestamp")
						.addFields(requiresFields()).get();

		readResponse(response);
		int len = (int)((stop - start)/step) + 1;
		List<TimePoint> ret = new ArrayList<TimePoint>();
		if (rawData.isEmpty()) return ret;
		List<Long> retTimes = new ArrayList<Long>();
		long curr=start;
		for (int i=0;i<len;i++) {
			retTimes.add(Long.valueOf(curr));
			curr += step;
		}
		Collection<L> preceeding = new ArrayList<L>();
		for (Long nextTime : retTimes) {
			long afterNextTime = nextTime + 1;
			preceeding = rawData.headMap(afterNextTime).values();
			rawData = rawData.tailMap(afterNextTime);
			List<L> tmplist = new ArrayList<L>(preceeding);
			if (tmplist.isEmpty()) {
				ret.add(new TimePoint(nextTime.longValue(), fillMissingValue()));
				continue;
			}
			ret.add(new TimePoint(nextTime.longValue(), estimateValue(tmplist, nextTime)));
		}
		return ret;
	}
	
	protected Number estimateValue(List<L> preceeding, Long stepTime) {
		return (Number) preceeding.get(preceeding.size() - 1);
	}

	protected Number fillMissingValue() {
		return 0;
	}
}
