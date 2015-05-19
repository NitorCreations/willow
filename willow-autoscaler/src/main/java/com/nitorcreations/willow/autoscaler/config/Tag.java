package com.nitorcreations.willow.autoscaler.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="Fields used via serialization")
public class Tag {

  public Tag(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public final String name;
  public final String value;

}
