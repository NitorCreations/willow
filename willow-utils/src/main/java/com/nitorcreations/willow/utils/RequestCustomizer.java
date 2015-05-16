package com.nitorcreations.willow.utils;

import java.net.URLConnection;

public interface RequestCustomizer {
  public void customize(URLConnection conn);
}
