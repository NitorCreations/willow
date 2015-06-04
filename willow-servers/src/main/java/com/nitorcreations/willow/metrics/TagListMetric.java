package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;

import com.nitorcreations.willow.messages.metrics.MetricConfig;

public abstract class TagListMetric implements Metric {
  @Inject
  protected Client client;
  private final String tagPrefix;
  public TagListMetric(String tagPrefix) {
    this.tagPrefix = tagPrefix;
  }
  @Override
  public List<String> calculateMetric(MetricConfig conf) {
    SearchRequestBuilder builder = client.prepareSearch(MetricUtils.getIndexes(conf.getStart(), conf.getStop(), client))
      .setTypes(conf.getTypes())
        .setSize(1000).addAggregation(AggregationBuilders.terms("tags")
          .field("tags").size(500).include(tagPrefix + "_.*"));
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
  public boolean hasData(MetricConfig conf) {
    return true;
  }
}