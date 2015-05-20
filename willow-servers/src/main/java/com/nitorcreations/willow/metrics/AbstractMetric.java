package com.nitorcreations.willow.metrics;

import java.util.List;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public abstract class AbstractMetric<T> implements Metric {

  public SearchResponse executeQuery(Client client, MetricConfig conf, String type, List<String> fields) {
    SearchRequestBuilder builder = client.prepareSearch(MetricUtils.getIndexes(conf.getStart(), conf.getStop(), client)).setTypes(type).setSearchType(SearchType.QUERY_AND_FETCH).setSize((int) (conf.getStop() - conf.getStart()) / 10);
    for (String next : fields) {
      builder.addField(next);
    }
    BoolQueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp").from(conf.getStart() - conf.getStep()).to(conf.getStop() + conf.getStep()).includeLower(false).includeUpper(true));
    for (String tag : conf.getTags()) {
      query = query.must(QueryBuilders.termQuery("tags", tag));
    }
    return builder.setQuery(query).get();
  }
}
