package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value={"URF_UNREAD_FIELD"}, justification="Field used in serialization")
public class SeriesData<T, L> {
  String key;
  List<Point<T, L>> values = new ArrayList<>();

  Map<T, L> pointsAsMap() {
    Map<T, L> ret = new LinkedHashMap<>();
    for (Point<T, L> next : values) {
      ret.put(next.x, next.y);
    }
    return ret;
  }
}
