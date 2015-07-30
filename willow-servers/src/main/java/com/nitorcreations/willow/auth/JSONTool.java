package com.nitorcreations.willow.auth;

import static java.util.Collections.unmodifiableSet;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;

enum JSONTool {
  ;

  static Set<String> toStringSet(JSONArray array) throws JSONException {
    Set<String> set = new LinkedHashSet<>();
    for(int i = 0; i < array.length(); i++) {
      set.add(array.getString(i));
    }
    return unmodifiableSet(set);
  }

  static JSONArray toArray(List<String> list1, List<String> list2) { // generics and varargs don't match..
    JSONArray array = new JSONArray();
    for(String s : list1) {
      array = array.put(s);
    }
    for(String s : list2) {
      array = array.put(s);
    }
    return array;
  }

}
