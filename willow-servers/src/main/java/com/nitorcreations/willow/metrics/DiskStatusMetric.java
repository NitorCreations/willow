package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.inject.Named;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.sort.SortOrder;

import com.nitorcreations.willow.messages.metrics.MetricConfig;


@Named("/disk")
public class DiskStatusMetric extends AbstractMetric<List<SeriesData<String, Long>>> {

  @Override
  public List<SeriesData<String, Long>> calculateMetric(MetricConfig conf) {
    SearchRequestBuilder builder = client.prepareSearch(MetricUtils.getIndexes(conf.getStart(), conf.getStop(), client)).setTypes(getType()).addField("timestamp").addField("name").addField("total").addField("free").setSize(50).addSort("timestamp", SortOrder.DESC);
    BoolQueryBuilder query = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("timestamp").from(conf.getStart()).to(conf.getStop()));
    for (String tag : conf.getTags()) {
      query = query.must(QueryBuilders.termQuery("tags", tag));
    }
    SearchResponse response = builder.setQuery(query).get();
    Map<String, long[]> data = new TreeMap<>();
    for (SearchHit next : response.getHits().getHits()) {
      Map<String, SearchHitField> results = next.getFields();
      String name = (String) results.get("name").getValue();
      long[] retRes = new long[2];
      retRes[0] = ((Number) results.get("total").value()).longValue();
      if (retRes[0] == 0)
        continue;
      retRes[1] = ((Number) results.get("free").value()).longValue();
      if (data.containsKey(name))
        break;
      data.put(name, retRes);
    }
    List<SeriesData<String, Long>> ret = new ArrayList<>();
    SeriesData<String, Long> used = new SeriesData<>();
    used.key = "used";
    SeriesData<String, Long> free = new SeriesData<>();
    free.key = "free";
    ret.add(used);
    ret.add(free);
    for (Entry<String, long[]> next : data.entrySet()) {
      Point<String, Long> usedFs = new Point<>();
      Point<String, Long> freeFs = new Point<>();
      usedFs.x = freeFs.x = next.getKey();
      usedFs.y = next.getValue()[0] - next.getValue()[1];
      freeFs.y = next.getValue()[1];
      used.values.add(usedFs);
      free.values.add(freeFs);
    }
    return ret;
  }

  @Override
  public String getType() {
    return "disk";
  }
}
