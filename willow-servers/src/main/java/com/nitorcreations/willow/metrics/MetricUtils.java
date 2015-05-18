package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.client.Client;

public abstract class MetricUtils {
  public static String[] getIndexes(long start, long end, Client client) {
    ArrayList<String> ret = new ArrayList<>();
    Calendar startCal = Calendar.getInstance();
    startCal.setTime(new Date(start));
    startCal.set(Calendar.SECOND, 1);
    startCal.set(Calendar.MINUTE, 0);
    startCal.set(Calendar.HOUR_OF_DAY, 0);
    Calendar endCal = Calendar.getInstance();
    endCal.setTime(new Date(end));
    while (startCal.before(endCal)) {
      String next = String.format("%04d", startCal.get(Calendar.YEAR)) + "-" + String.format("%02d", (startCal.get(Calendar.MONTH) + 1)) + "-" + String.format("%02d", startCal.get(Calendar.DAY_OF_MONTH));
      client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();
      ClusterStateResponse response = client.admin().cluster().prepareState().execute().actionGet();
      boolean hasIndex = response.getState().metaData().hasIndex(next);
      if (hasIndex) {
        ret.add(next);
      }
      startCal.add(Calendar.DAY_OF_YEAR, 1);
    }
    return ret.toArray(new String[ret.size()]);
  }
  public static <Y extends Comparable> Y median(List<Y> data) {
    Collections.sort(data);
    return data.get(data.size() / 2);
  }

}
