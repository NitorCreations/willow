package com.nitorcreations.willow.logstash;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.ConvertUtils;

import com.google.gson.Gson;
import com.nitorcreations.willow.messages.AbstractMessage;
import com.nitorcreations.willow.messages.MessageMapping;

public class Adapter {
  public Adapter() {  }
  public AbstractMessage fromHash(String messageType, Map<String, Object> rubyHash) {
    Class<? extends AbstractMessage> cls = MessageMapping.map(messageType.toLowerCase());
    if (cls == null) return null;
    try {
      AbstractMessage ret = cls.getConstructor(new Class[0]).newInstance();
      for (Entry<String, Object> next : rubyHash.entrySet()) {
        String key = next.getKey();
        if (key.startsWith("@")) {
          key = key.substring(1);
        }
        if (Character.isUpperCase(key.charAt(0))) {
          String start = key.substring(0, 1).toLowerCase();
          key = start + key.substring(1);
        }
        try {
          Field nextField = ret.getClass().getField(key);
          nextField.set(ret, ConvertUtils.convert(next.getValue(), nextField.getType()));
        } catch (NoSuchFieldException | SecurityException
            | IllegalArgumentException | IllegalAccessException e) {
          ret.setExtra(key, next.getValue().toString());
        }
      }
      if (System.getProperty("debug") != null) {
        System.out.println(new Gson().toJson(ret));
      }
      return ret;
    } catch (InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException e) {
      return null;
    }
  }
}
