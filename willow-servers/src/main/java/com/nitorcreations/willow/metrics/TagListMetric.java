package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;

public abstract class TagListMetric implements Metric {
  private final String tagPrefix;
  public TagListMetric(String tagPrefix) {
    this.tagPrefix = tagPrefix;
  }

  @Override
  public List<String> calculateMetric(Client client, MetricConfig conf) {
    SearchRequestBuilder builder = client.prepareSearch(MetricUtils.getIndexes(conf.getStart(), conf.getStop(), client))
      .setTypes(conf.getTypes())
        .setSize(0).addAggregation(AggregationBuilders.terms("tags")
          .field("tags").include(tagPrefix + "_.*"));
    BoolQueryBuilder query = QueryBuilders.boolQuery()
      .must(QueryBuilders.rangeQuery("timestamp").from(conf.getStart()).to(conf.getStop()));
    for (String tag : conf.getTags()) {
      query = query.must(QueryBuilders.termQuery("tags", tag));
    }
    SearchResponse response = builder.setQuery(query).get();
    ArrayList<String> ret = new ArrayList<>();
    if (response != null && response.getAggregations() != null) {
      InternalTerms agg = response.getAggregations().get("tags");
      if (agg != null) {
        for (Bucket next : agg.getBuckets()) {
          ret.add(next.getKey());
        }
      }
    }
    return ret;
  }
}