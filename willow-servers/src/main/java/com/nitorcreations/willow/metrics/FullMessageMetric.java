package com.nitorcreations.willow.metrics;

import java.lang.reflect.ParameterizedType;
import java.util.SortedMap;
import java.util.TreeMap;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import com.google.gson.Gson;
import com.nitorcreations.willow.messages.AbstractMessage;
import com.nitorcreations.willow.messages.MessageMapping;

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
  public R calculateMetric(Client client, MetricConfig conf) {
    SearchResponse response = executeQuery(client, conf, MessageMapping.map(type).lcName());
    readResponse(response);
    return processData(conf.getStart(), conf.getStop(), conf.getStep(), conf);
  }
  protected abstract R processData(long start, long stop, int step, MetricConfig conf);
}
