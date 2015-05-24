package com.nitorcreations.willow.metrics;

import java.util.Arrays;
import java.util.List;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.nitorcreations.willow.messages.metrics.MetricConfig;

public abstract class AbstractMetric<T> implements Metric {
  public static final BuilderCustomizer ONE = new OneResultBuilderCustomizer();

  public SearchResponse executeQuery(Client client, MetricConfig conf, String type, List<String> fields) {
    return this.executeQuery(client, conf, type, fields, null);
  }
  public SearchResponse executeQuery(Client client, MetricConfig conf, String type, List<String> fields, BuilderCustomizer customizer) {
    SearchRequestBuilder builder = client.prepareSearch(MetricUtils.getIndexes(conf.getStart(), conf.getStop(), client))
        .setTypes(type).setSearchType(SearchType.QUERY_AND_FETCH)
        .setSize((int) (conf.getStop() - conf.getStart()) / 10);
    for (String next : fields) {
      builder.addField(next);
    }
    BoolQueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp").from(conf.getStart() - conf.getStep()).to(conf.getStop()).includeLower(false).includeUpper(true));
    for (String tag : conf.getTags()) {
      query = query.must(QueryBuilders.termQuery("tags", tag));
    }
    builder.setQuery(query);
    if (customizer != null) {
      customizer.customize(builder);
    }
    return builder.get();
  }
  @Override
  public boolean hasData(Client client, MetricConfig conf) {
    SearchResponse response = executeQuery(client, conf, getType(), Arrays.asList("timestamp"), AbstractMetric.ONE);
    return response.getHits().getHits().length > 0;
  }
  public abstract String getType();
}
