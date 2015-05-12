package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

public abstract class SimpleMetric<L, T> implements Metric {
  protected SortedMap<Long, L> rawData;
  private Map<String, SearchHitField> fields;
  protected MetricConfig conf;
  public abstract String getType();

  public abstract String[] requiresFields();

  protected void readResponse(SearchResponse response) {
    rawData = new TreeMap<Long, L>();
    for (SearchHit next : response.getHits().getHits()) {
      fields = next.getFields();
      Long timestamp = fields.get("timestamp").value();
      List<T> nextData = new ArrayList<>();
      for (String nextField : requiresFields()) {
        if (fields.get(nextField) == null)
          break;
        nextData.add((T) fields.get(nextField).value());
      }
      if (nextData.size() == requiresFields().length) {
        rawData.put(timestamp, getValue(nextData));
      }
    }
  }

  @SuppressWarnings("unchecked")
  protected L getValue(List<T> arr) {
    return (L) arr.get(0);
  }

  @Override
  public List<TimePoint> calculateMetric(Client client, MetricConfig conf) {
    this.conf = conf;
    SearchRequestBuilder builder = client.prepareSearch(MetricUtils.getIndexes(conf.start, conf.stop, client)).setTypes(getType()).setSearchType(SearchType.QUERY_AND_FETCH).setSize((int) (conf.stop - conf.start) / 10).addField("timestamp").addFields(requiresFields());
    BoolQueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp").from(conf.start - conf.step).to(conf.stop + conf.step).includeLower(false).includeUpper(true));
    for (String tag : conf.tags) {
      query = query.must(QueryBuilders.termQuery("tags", tag));
    }
    SearchResponse response = builder.setQuery(query).get();
    readResponse(response);
    int len = (int) ((conf.stop - conf.start) / conf.step) + 1;
    List<TimePoint> ret = new ArrayList<TimePoint>();
    if (rawData.isEmpty())
      return ret;
    List<Long> retTimes = new ArrayList<Long>();
    long curr = conf.start;
    for (int i = 0; i < len; i++) {
      retTimes.add(Long.valueOf(curr));
      curr += conf.step;
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
      ret.add(new TimePoint(nextTime.longValue(), estimateValue(tmplist, nextTime, conf.step, conf)));
    }
    return ret;
  }

  protected Number estimateValue(List<L> preceeding, long stepTime, long stepLen, MetricConfig conf) {
    return (Number) preceeding.get(preceeding.size() - 1);
  }

  protected Number fillMissingValue() {
    return 0;
  }
}
