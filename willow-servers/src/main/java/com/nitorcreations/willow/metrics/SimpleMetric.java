package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import com.nitorcreations.willow.messages.metrics.MetricConfig;
import com.nitorcreations.willow.messages.metrics.TimePoint;

public abstract class SimpleMetric<L extends Comparable, T> extends AbstractMetric<T> {
  protected SortedMap<Long, L> rawData = new TreeMap<Long, L>();
  private Map<String, SearchHitField> fields;
  public abstract String getType();

  public abstract String[] requiresFields();

  protected void readResponse(SearchResponse response) {
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
  public List<TimePoint<L>> calculateMetric(Client client, MetricConfig conf) {
    List<String> fields = new ArrayList<String>();
    fields.add("timestamp");
    fields.addAll(Arrays.asList(requiresFields()));
    SearchResponse response = executeQuery(client, conf, getType(), fields);
    readResponse(response);
    int len = (int) ((conf.getStop() - conf.getStart()) / conf.getStep()) + 1;
    List<TimePoint<L>> ret = new ArrayList<TimePoint<L>>();
    if (rawData.isEmpty())
      return ret;
    List<Long> retTimes = new ArrayList<Long>();
    long curr = conf.getStart();
    for (int i = 0; i < len; i++) {
      retTimes.add(Long.valueOf(curr));
      curr += conf.getStep();
    }
    for (Long nextTime : retTimes) {
      long afterNextTime = nextTime + 1;
      Collection<L> preceeding = rawData.headMap(afterNextTime).values();
      rawData = rawData.tailMap(afterNextTime);
      List<L> tmplist = new ArrayList<L>(preceeding);
      if (tmplist.isEmpty()) {
        ret.add(new TimePoint(nextTime.longValue(), fillMissingValue()));
        continue;
      }
      ret.add(new TimePoint(nextTime.longValue(), estimateValue(tmplist, nextTime, conf.getStep(), conf)));
    }
    return ret;
  }

  protected L estimateValue(List<L> preceeding, long stepTime, long stepLen, MetricConfig conf) {
    return (L) preceeding.get(preceeding.size() - 1);
  }

  protected abstract L fillMissingValue();
}
