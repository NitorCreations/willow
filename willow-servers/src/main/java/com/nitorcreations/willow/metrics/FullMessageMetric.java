package com.nitorcreations.willow.metrics;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;

import com.nitorcreations.willow.messages.AbstractMessage;
import com.nitorcreations.willow.messages.MessageMapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

public abstract class FullMessageMetric<T extends AbstractMessage, X extends Comparable<X>, Y extends Comparable<Y>> implements Metric {
  protected SortedMap<Long, T> rawData;
  private final Class<T> type;

  @SuppressWarnings("unchecked")
  public FullMessageMetric() {
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
  public Collection<SeriesData<X, Y>> calculateMetric(Client client, HttpServletRequest req) {
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
    Map<String, SeriesData<X, Y>> ret = new LinkedHashMap<String, SeriesData<X, Y>>();
    if (rawData.isEmpty())
      return ret.values();
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
      addValue(ret, tmplist, nextTime.longValue(), step);
    }
    fillMissingValues(ret, retTimes, step);
    return ret.values();
  }

  protected abstract void addValue(Map<String, SeriesData<X, Y>> values, List<T> preeceding, long stepTime, long stepLen);

  @SuppressWarnings("unchecked")
  protected void fillMissingValues(Map<String, SeriesData<X, Y>> ret, List<Long> retTimes, long stepLen) {
    for (SeriesData<X, Y> nextValues : ret.values()) {
      Map<X, Y> valueMap = nextValues.pointsAsMap();
      List<Long> addX = new ArrayList<>();
      for (Long nextStep : retTimes) {
        if (!valueMap.containsKey(nextStep)) {
          addX.add(nextStep);
        }
      }
      Object zero = 0;
      for (Long nextAdd : addX) {
        for (int i = 0; i < nextValues.values.size(); i++) {
          if (nextValues.values.get(i).x.compareTo((X) nextAdd) > 0) {
            Point<X, Y> toAdd = new Point<>();
            toAdd.x = (X) nextAdd;
            toAdd.y = (Y) zero;
            nextValues.values.add(i, toAdd);
            break;
          }
        }
      }
    }
  }

  protected Y median(List<Y> data) {
    Collections.sort(data);
    return data.get(data.size() / 2);
  }
}
