package com.nitorcreations.willow.utils;

import java.net.URLConnection;

public interface RequestCustomizer {
  void customize(URLConnection conn);
}
