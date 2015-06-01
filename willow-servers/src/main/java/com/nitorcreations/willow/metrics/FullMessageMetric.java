package com.nitorcreations.willow.metrics;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import com.google.gson.Gson;
import com.nitorcreations.willow.messages.AbstractMessage;
import com.nitorcreations.willow.messages.MessageMapping;
import com.nitorcreations.willow.messages.metrics.MetricConfig;

public abstract class FullMessageMetric<T extends AbstractMessage, R> extends AbstractMetric<T> {
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
      nextMsg.setId(next.getId());
      rawData.put(nextMsg.getTimestamp(), nextMsg);
    }
  }
  @Override
  public R calculateMetric(MetricConfig conf) {
    SearchResponse response = executeQuery(conf, MessageMapping.map(type).lcName(), Collections.<String>emptyList(), getCustomizer());
    readResponse(response);
    return processData(conf.getStart(), conf.getStop(), conf.getStep(), conf);
  }

  public String getType() {
    return MessageMapping.map(this.type).lcName();
  }
  protected BuilderCustomizer getCustomizer() {
    return null;
  }
  protected abstract R processData(long start, long stop, int step, MetricConfig conf);
}
