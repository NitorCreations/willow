package com.nitorcreations.willow;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class GsonTest {
  public static void main(String[] args) {
    HashMap<String, String> tags = new HashMap<>();
    tags.put("foo", "bar");
    tags.put("baz", "dope");
    tags.put("hum", "100");
    String json = "{ \"instance\": \"foo\", \"tags\": " + new Gson().toJson(tags) + "}";
    System.out.println(json);
    FromJson fJson = new Gson().fromJson(json, FromJson.class);
    System.out.println(fJson);
  }

  public class FromJson {
    public String instance;
    public Map<String, String> tags;

    public String toString() {
      return new Gson().toJson(this);
    }
  }
}
