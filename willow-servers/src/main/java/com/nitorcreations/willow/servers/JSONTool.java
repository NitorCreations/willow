package com.nitorcreations.willow.servers;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

enum JSONTool {
    ;

    static Set<String> toStringSet(JSONArray array) throws JSONException {
        Set<String> set = new HashSet<>();
        for(int i = 0; i < array.length(); i++) {
            set.add(array.getString(i));
        }
        return unmodifiableSet(set);
    }

    static JSONArray toArray(List<String> list1, List<String> list2) { // generics and varargs don't match..
        List<String> merged = new ArrayList<>(list1);
        merged.addAll(list2);
        return new JSONArray(merged);
    }


}
