package com.nitorcreations.willow.deployer.statistics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.inject.Named;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nitorcreations.willow.deployer.download.StreamPumper;
import com.nitorcreations.willow.messages.LongStatisticsMessage;

@Named("varnish")
public class VarnishStats extends AbstractStatisticsSender {
  private long interval;
  private long nextStats;

  @Override
  public void execute() {
    long now = System.currentTimeMillis();
    try {
      if (now > nextStats) {
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/varnishstat", "-j");
        pb.environment().putAll(System.getenv());
        Process p = pb.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        StreamPumper stdout = new StreamPumper(p.getInputStream(), out);
        StreamPumper stderr = new StreamPumper(p.getErrorStream(), err);
        new Thread(stdout, "stdout").start();
        new Thread(stderr, "stderr").start();
        int ret = p.waitFor();
        if (ret == 0) {
          Map<String, Long> values = new LinkedHashMap<String, Long>();
          JsonObject j = new JsonParser().parse(new String(out.toByteArray(), StandardCharsets.UTF_8)).getAsJsonObject();
          for (Entry<String, JsonElement> entry : j.entrySet()) {
            if (entry.getValue().isJsonObject()) {
              values.put(entry.getKey(), entry.getValue().getAsJsonObject().get("value").getAsLong());
            }
          }
          LongStatisticsMessage send = new LongStatisticsMessage();
          send.setMap(values);
          send.addTags("category_varnish");
          transmitter.queue(send);
        } else {
          logger.log(Level.INFO, "varnishstat returned " + ret + "\n" +  new String(out.toByteArray(), StandardCharsets.UTF_8) + "\n"
             + new String(err.toByteArray(), StandardCharsets.UTF_8));
        }
        nextStats = now + interval;
      }
      TimeUnit.MILLISECONDS.sleep(nextStats - now);
    } catch (InterruptedException | IOException e) {
      logger.log(Level.INFO, "Exception while getting varhish statistics", e);
      return;
    }
  }
  @Override
  public void setProperties(Properties properties) {
    interval = Long.parseLong(properties.getProperty("interval", "5000"));
    nextStats = System.currentTimeMillis() + interval;
  }
}
