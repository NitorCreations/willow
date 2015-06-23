package com.nitorcreations.willow.auth;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;

public class JSONToolTest {
  @Test
  public void testToArrayToStringSet() throws JSONException {
    List<String> lst1 = Arrays.asList(new String[] { "a", "b" });
    List<String> lst2 = Arrays.asList(new String[] { "c", "d" });
    JSONArray arr = JSONTool.toArray(lst1, lst2);
    Set<String> ret = JSONTool.toStringSet(arr);
    assertArrayEquals(new String[] { "a", "b", "c", "d" }, ret.toArray()); 
  }
}
