package com.nitorcreations.willow.metrics;

import java.lang.reflect.ParameterizedType;
import java.util.SortedMap;
import java.util.TreeMap;

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

public abstract class FullMessageMetric<T extends AbstractMessage, R> implements Metric {
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
      rawData.put(nextMsg.timestamp, nextMsg);
    }
  }
  @Override
  public R calculateMetric(Client client, MetricConfig conf) {
    SearchRequestBuilder builder = client.prepareSearch(MetricUtils.getIndexes(conf.getStart(), conf.getStop(), client)).setTypes(MessageMapping.map(type).lcName()).setSearchType(SearchType.QUERY_AND_FETCH).setSize((int) (conf.getStop() - conf.getStart()) / 10);
    BoolQueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp").from(conf.getStart() - conf.getStep()).to(conf.getStop() + conf.getStep()).includeLower(false).includeUpper(true));
    for (String tag : conf.getTags()) {
      query = query.must(QueryBuilders.termQuery("tags", tag));
    }
    SearchResponse response = builder.setQuery(query).get();
    readResponse(response);
    return processData(conf.getStart(), conf.getStop(), conf.getStep(), conf);
  }
  protected abstract R processData(long start, long stop, int step, MetricConfig conf);
}
