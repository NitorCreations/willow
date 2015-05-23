package com.nitorcreations.willow.messages.metrics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimePoint<Y extends Comparable> {
  private String time;
  private Y value;

  public TimePoint(long millis, Y value) {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    df.setTimeZone(tz);
    time = df.format(new Date(millis));
    this.value = value;
  }

  public TimePoint(TimePoint<Y> toCopy) {
    this.time = toCopy.time;
    this.value = toCopy.value;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public Y getValue() {
    return value;
  }

  public void setValue(Y value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "{ \"time\": " + time + "\"value\":"+ value.toString() + " }";
  }
}
