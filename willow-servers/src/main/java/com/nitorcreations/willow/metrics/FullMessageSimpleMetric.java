package com.nitorcreations.willow.metrics;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.google.gson.Gson;
import com.nitorcreations.willow.messages.AbstractMessage;
import com.nitorcreations.willow.messages.MessageMapping;

public abstract class FullMessageSimpleMetric<T extends AbstractMessage> implements Metric {
  protected SortedMap<Long, T> rawData;
  private final Class<T> type;

  @SuppressWarnings("unchecked")
  public FullMessageSimpleMetric() {
    this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
  }

  protected void readResponse(SearchResponse response) {
    rawData = new TreeMap<Long, T>();
    Gson gson = new Gson();
    for (SearchHit next : response.getHits().getHits()) {
      T nextMsg = gson.fromJson(next.getSourceAsString(), type);
      rawData.put(nextMsg.timestamp, nextMsg);
    }
  }

  @Override
  public Collection<TimePoint> calculateMetric(Client client, HttpServletRequest req) {
    long start = Long.parseLong(req.getParameter("start"));
    long stop = Long.parseLong(req.getParameter("stop"));
    int step = Integer.parseInt(req.getParameter("step"));
    String[] tags = req.getParameterValues("tag");
    SearchRequestBuilder builder = client.prepareSearch(MetricUtils.getIndexes(start, stop, client)).setTypes(MessageMapping.map(type).lcName()).setSearchType(SearchType.QUERY_AND_FETCH).setSize((int) (stop - start) / 10);
    BoolQueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp").from(start - step).to(stop + step).includeLower(false).includeUpper(true));
    for (String tag : tags) {
      query = query.must(QueryBuilders.termQuery("tags", tag));
    }
    SearchResponse response = builder.setQuery(query).get();
    readResponse(response);
    int len = (int) ((stop - start) / step) + 1;
    List<TimePoint> ret = new ArrayList<TimePoint>();
    if (rawData.isEmpty())
      return ret;
    List<Long> retTimes = new ArrayList<Long>();
    long curr = start;
    for (int i = 0; i < len; i++) {
      retTimes.add(Long.valueOf(curr));
      curr += step;
    }
    Collection<T> preceeding = new ArrayList<T>();
    for (Long nextTime : retTimes) {
      long afterNextTime = nextTime + 1;
      preceeding = rawData.headMap(afterNextTime).values();
      rawData = rawData.tailMap(afterNextTime);
      List<T> tmplist = new ArrayList<T>(preceeding);
      if (tmplist.isEmpty()) {
        ret.add(new TimePoint(nextTime.longValue(), fillMissingValue()));
        continue;
      }
      ret.add(new TimePoint(nextTime.longValue(), estimateValue(tmplist, nextTime, step)));
    }
    return ret;
  }
  protected Number fillMissingValue() {
    return 0D;
  }
  protected abstract Number estimateValue(List<T> preceeding, long stepTime, long stepLen);

  protected <Y extends Comparable> Y median(List<Y> data) {
    Collections.sort(data);
    return data.get(data.size() / 2);
  }
}
