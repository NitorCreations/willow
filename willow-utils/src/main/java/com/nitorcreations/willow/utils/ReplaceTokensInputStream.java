package com.nitorcreations.willow.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplaceTokensInputStream extends FilterInputStream {
  public static class StartEndDelimeters {
    public final String start;
    public final String end;

    public StartEndDelimeters(String start, String end) {
      this.start = start;
      this.end = end;
    }
  }
  private final StartEndDelimeters[] delimiters;
  private final Map<String, String> tokens;
  private final Charset charset;
  private int shortestTokenLength = Integer.MAX_VALUE;
  private int longestTokenLength = 0;
  private int shortestDelimeterLength = Integer.MAX_VALUE;
  private int longestDelimeterLength = 0;
  private boolean streamExhausted = false;
  private List<Integer> available = new ArrayList<>();
  private int length = 0;
  private int start = 0;
  private final byte[] buffer;
  public static final StartEndDelimeters AT_DELIMITERS = new StartEndDelimeters("@", "@");
  public static final StartEndDelimeters CURLY_DELIMITERS = new StartEndDelimeters("${", "}");
  public static final StartEndDelimeters[] MAVEN_DELIMITERS = new StartEndDelimeters[] { AT_DELIMITERS, CURLY_DELIMITERS };

  public ReplaceTokensInputStream(InputStream in, Map<String, String> tokens, StartEndDelimeters... delimiters) {
    this(in, Charset.defaultCharset(), tokens, delimiters);
  }

  public ReplaceTokensInputStream(InputStream in, Charset charset, Map<String, String> tokens, StartEndDelimeters... delimiters) {
    super(in);
    this.delimiters = delimiters;
    this.tokens = tokens;
    this.charset = charset;
    for (String next : tokens.keySet()) {
      int nextLen = next.getBytes(charset).length;
      if (nextLen < shortestTokenLength) {
        shortestTokenLength = nextLen;
      }
      if (nextLen > longestTokenLength) {
        longestTokenLength = nextLen;
      }
    }
    for (StartEndDelimeters next : delimiters) {
      int nextLen = (next.start + next.end).getBytes(charset).length;
      if (nextLen < shortestDelimeterLength) {
        shortestDelimeterLength = nextLen;
      }
      if (nextLen > longestDelimeterLength) {
        longestDelimeterLength = nextLen;
      }
    }
    buffer = new byte[longestDelimeterLength + longestTokenLength];
  }

  @Override
  public int read() throws IOException {
    if (!available.isEmpty()) {
      return available.remove(0);
    }
    if (!streamExhausted) {
      append(superRead());
    } else if (length < 1) {
      return -1;
    } else {
      shift(length, true);
      return available.remove(0);
    }
    String bufferStr = getBufferString();
    List<StartEndDelimeters> potentialMatches = getPotentialMatches(bufferStr);
    if (potentialMatches.isEmpty()) {
      shift(1, true);
      return available.remove(0);
    }
    String matchToken = null;
    do {
      matchToken = getMatchToken(bufferStr, potentialMatches);
      if (potentialMatches.isEmpty())
        break;
      if (matchToken != null) {
        for (byte next : matchToken.getBytes(charset)) {
          available.add((int) next & 0xFF);
        }
        if (available.size() > 0) {
          return available.remove(0);
        }
      }
      if (length == buffer.length)
        break;
      append(superRead());
      bufferStr = getBufferString();
      if (matchToken != null) {
        // Returned empty token value and did not have available bytes. Need to reset and try again
        matchToken = null;
        potentialMatches = getPotentialMatches(bufferStr);
        if (potentialMatches.isEmpty()) {
          shift(1, true);
          return available.remove(0);
        }
      }
    } while (matchToken == null && !streamExhausted);
    if (length < 1 && available.isEmpty())
      return -1;
    shift(1, true);
    return available.remove(0);
  }

  private void append(int next) {
    if (next != -1) {
      buffer[(start + length) % buffer.length] = (byte) (next & 0xFF);
      ++length;
    }
  }

  private void shift(int shift, boolean makeAvailable) {
    int doShift = Math.min(length, shift);
    if (makeAvailable) {
      for (int i = 0; i < doShift; i++) {
        available.add((int) buffer[(start + i) % buffer.length] & 0xFF);
      }
    }
    length -= doShift;
    start = (start + doShift) % buffer.length;
  }

  private String getBufferString() {
    StringBuilder ret = new StringBuilder();
    int firstLen = Math.min(buffer.length - start, length);
    ret.append(new String(buffer, start, firstLen, charset));
    if (firstLen < length) {
      ret.append(new String(buffer, 0, length - firstLen, charset));
    }
    return ret.toString();
  }

  private void eatUpToAndIncluding(int startLen, String delimiter, boolean makeAvailable) {
    shift(startLen, makeAvailable);
    while (!getBufferString().startsWith(delimiter)) {
      shift(1, makeAvailable);
    }
    shift(delimiter.getBytes(charset).length, makeAvailable);
  }

  private List<StartEndDelimeters> getPotentialMatches(String bufferStr) {
    List<StartEndDelimeters> ret = new ArrayList<>();
    for (StartEndDelimeters next : delimiters) {
      int len = Math.min(next.start.length(), bufferStr.length());
      if (next.start.substring(0, len).equals(bufferStr.subSequence(0, len))) {
        ret.add(next);
      }
    }
    return ret;
  }

  private String getMatchToken(String bufferStr, List<StartEndDelimeters> potentialMatches) {
    for (StartEndDelimeters next : potentialMatches.toArray(new StartEndDelimeters[potentialMatches.size()])) {
      int nextMinLength = next.start.length() + next.end.length() + 1;
      if (bufferStr.length() >= nextMinLength && bufferStr.startsWith(next.start)) {
        int endIndex = bufferStr.indexOf(next.end, next.start.length() + 1);
        if (endIndex > 0) {
          String ret = tokens.get(bufferStr.substring(next.start.length(), endIndex));
          if (ret != null) {
            eatUpToAndIncluding(next.start.getBytes(charset).length, next.end, false);
            return ret;
          }
        }
      } else if (bufferStr.length() >= next.start.length() && !bufferStr.startsWith(next.start)) {
        potentialMatches.remove(next);
      }
    }
    return null;
  }

  private int superRead() throws IOException {
    int ret = super.read();
    if (ret == -1) {
      streamExhausted = true;
    }
    return ret;
  }
}
