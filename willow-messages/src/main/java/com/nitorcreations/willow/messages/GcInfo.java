package com.nitorcreations.willow.messages;

import org.msgpack.annotation.Message;

@Message
public class GcInfo {
  String name;
  long collectionCount;
  long collectionTime;

  public long getCollectionCount() {
    return collectionCount;
  }

  public void setCollectionCount(long collectionCount) {
    this.collectionCount = collectionCount;
  }

  public long getCollectionTime() {
    return collectionTime;
  }

  public void setCollectionTime(long collectionTime) {
    this.collectionTime = collectionTime;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
