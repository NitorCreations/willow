package com.nitorcreations.willow.deployer.statistics;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

import com.nitorcreations.willow.messages.event.MetricThresholdTriggeredEvent;

@Named("event")
public class EventSender extends AbstractStatisticsSender {

  private Logger logger = Logger.getLogger(getClass().getName());

  @Override
  public void execute() {
    //note that this is just a test class that sends a hardcoded event...
    MetricThresholdTriggeredEvent mte = new MetricThresholdTriggeredEvent();
    mte.description = "event description";
    mte.metric = "myMetric";
    mte.threshold = 10D;
    mte.value = 11D;
    logger.finest("Sending event");

    transmitter.queue(mte.getEventMessage());
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e1) {
      logger.log(Level.FINE, "Interrupted", e1);
    }
  }

  @Override
  public void setProperties(Properties properties) {

  }
}
