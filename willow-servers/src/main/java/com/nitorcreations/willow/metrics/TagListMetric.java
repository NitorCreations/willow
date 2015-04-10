package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

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
  public List<String> calculateMetric(Client client, HttpServletRequest req) {
    long start = Long.parseLong(req.getParameter("start"));
    long stop = Long.parseLong(req.getParameter("stop"));
    String[] types = req.getParameterValues("type");
    String[] andTags = req.getParameterValues("tag");
    SearchRequestBuilder builder = client.prepareSearch(MetricUtils.getIndexes(start, stop, client))
      .setTypes(types)
        .setSize(0).addAggregation(AggregationBuilders.terms("tags")
          .field("tags").include(tagPrefix + "_.*"));
    BoolQueryBuilder query = QueryBuilders.boolQuery()
      .must(QueryBuilders.rangeQuery("timestamp").from(start).to(stop));
    if (andTags != null) {
      for (String tag : andTags) {
        query = query.must(QueryBuilders.termQuery("tags", tag));
      }
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