/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nitorcreations.willow.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.parser.ParserException;


/**
 * Base class for Yaml factories.
 *
 * @author Dave Syer
 * @since 4.1
 */
public class YamlProcessor {

  private final Logger logger = Logger.getLogger(getClass().getCanonicalName());

  private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;

  private InputStream[] resources = new InputStream[0];

  private List<DocumentMatcher> documentMatchers = Collections.emptyList();

  private boolean matchDefault = true;


  /**
   * A map of document matchers allowing callers to selectively use only
   * some of the documents in a YAML URI. In YAML documents are
   * separated by <code>---<code> lines, and each document is converted
   * to properties before the match is made. E.g.
   *
   * <pre class="code">
   * environment: dev
   * url: http://dev.bar.com
   * name: Developer Setup
   * ---
   * environment: prod
   * url:http://foo.bar.com
   * name: My Cool App
   * </pre>
   *
   * when mapped with
   * <code>documentMatchers = YamlProcessor.mapMatcher({"environment": "prod"})</code>
   * would end up as
   *
   * <pre class="code">
   * environment=prod
   * url=http://foo.bar.com
   * name=My Cool App
   * url=http://dev.bar.com
   * </pre>
   * @param matchers a map of keys to value patterns (regular expressions)
   */
  public void setDocumentMatchers(DocumentMatcher... matchers) {
    this.documentMatchers = Arrays.asList(matchers);
  }

  /**
   * Flag indicating that a document for which all the
   * {@link #setDocumentMatchers(DocumentMatcher...) document matchers} abstain will
   * nevertheless match.
   * @param matchDefault the flag to set (default true)
   */
  public void setMatchDefault(boolean matchDefault) {
    this.matchDefault = matchDefault;
  }

  /**
   * Method to use for resolving URIs. Each URI will be converted to a Map, so
   * this property is used to decide which map entries to keep in the final output from
   * this factory.
   * @param resolutionMethod the resolution method to set (defaults to
   * {@link ResolutionMethod#OVERRIDE}).
   */
  public void setResolutionMethod(ResolutionMethod resolutionMethod) {
    this.resolutionMethod = resolutionMethod;
  }

  /**
   * Set locations of YAML {@link String URIs} to be loaded.
   * @see ResolutionMethod
   */
  public void setResources(InputStream... resources) {
    this.resources = resources;
  }


  /**
   * Provide an opportunity for subclasses to process the Yaml parsed from the supplied
   * URIs. Each URI is parsed in turn and the documents inside checked against
   * the {@link #setDocumentMatchers(DocumentMatcher...) matchers}. If a document
   * matches it is passed into the callback, along with its representation as
   * Properties. Depending on the {@link #setResolutionMethod(ResolutionMethod)} not all
   * of the documents will be parsed.
   * @param callback a callback to delegate to once matching documents are found
   * @see #createYaml()
   */
  protected void process(MatchCallback callback) {
    Yaml yaml = createYaml();
    for (InputStream next : this.resources) {
      boolean found = process(callback, yaml, next);
      if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
        return;
      }
    }
  }

  /**
   * Create the {@link Yaml} instance to use.
   */
  protected Yaml createYaml() {
    return new Yaml(new StrictMapAppenderConstructor());
  }

  private boolean process(MatchCallback callback, Yaml yaml, InputStream resource) {
    int count = 0;
    try {
      if (this.logger.isLoggable(Level.FINE)) {
        this.logger.fine("Loading from YAML: " + resource);
      }
      try (InputStream stream = resource) {
        for (Object object : yaml.loadAll(stream)) {
          if (object != null && process(asMap(object), callback)) {
            count++;
            if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
              break;
            }
          }
        }
        if (this.logger.isLoggable(Level.FINE)) {
          this.logger.fine("Loaded " + count + " document" + (count > 1 ? "s" : "") +
              " from YAML URI: " + resource);
        }
      }
    } catch (IOException ex) {
      handleProcessError(resource, ex);
    }
    return count > 0;
  }

  private void handleProcessError(InputStream resource, IOException ex) {
    if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND &&
        this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
      throw new IllegalStateException(ex);
    }
    this.logger.warning("Could not load map from " + resource + ": " + ex.getMessage());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(Object object) {
    // YAML can have numbers as keys
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    if (!(object instanceof Map)) {
      // A document can be a text literal
      result.put("document", object);
      return result;
    }

    Map<Object, Object> map = (Map<Object, Object>) object;
    for (Entry<Object, Object> entry : map.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Map) {
        value = asMap(value);
      }
      Object key = entry.getKey();
      if (key instanceof CharSequence) {
        result.put(key.toString(), value);
      }
      else {
        // It has to be a map key in this case
        result.put("[" + key.toString() + "]", value);
      }
    }
    return result;
  }

  private boolean process(Map<String, Object> map, MatchCallback callback) {
    Properties properties = new Properties();
    properties.putAll(getFlattenedMap(map));

    if (this.documentMatchers.isEmpty()) {
      if (this.logger.isLoggable(Level.FINE)) {
        this.logger.fine("Merging document (no matchers set)" + map);
      }
      callback.process(properties, map);
      return true;
    }

    MatchStatus result = MatchStatus.ABSTAIN;
    for (DocumentMatcher matcher : this.documentMatchers) {
      MatchStatus match = matcher.matches(properties);
      result = MatchStatus.getMostSpecific(match, result);
      if (match == MatchStatus.FOUND) {
        if (this.logger.isLoggable(Level.FINE)) {
          this.logger.fine("Matched document with document matcher: " + properties);
        }
        callback.process(properties, map);
        return true;
      }
    }

    if (result == MatchStatus.ABSTAIN && this.matchDefault) {
      if (this.logger.isLoggable(Level.FINE)) {
        this.logger.fine("Matched document with default matcher: " + map);
      }
      callback.process(properties, map);
      return true;
    }

    this.logger.fine("Unmatched document");
    return false;
  }

  /**
   * Return a flattened version of the given map, recursively following any nested Map
   * or Collection values. Entries from the resulting map retain the same order as the
   * source. When called with the Map from a {@link MatchCallback} the result will
   * contain the same values as the {@link MatchCallback} Properties.
   * @param source the source map
   * @return a flattened map
   * @since 4.1.3
   */
  protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    buildFlattenedMap(result, source, null);
    return result;
  }

  private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
    for (Entry<String, Object> entry : source.entrySet()) {
      String key = entry.getKey();
      if (path != null && !path.trim().isEmpty()) {
        if (key.startsWith("[")) {
          key = path + key;
        }
        else {
          key = path + "." + key;
        }
      }
      Object value = entry.getValue();
      if (value instanceof String) {
        result.put(key, value);
      }
      else if (value instanceof Map) {
        // Need a compound key
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        buildFlattenedMap(result, map, key);
      }
      else if (value instanceof Collection) {
        // Need a compound key
        @SuppressWarnings("unchecked")
        Collection<Object> collection = (Collection<Object>) value;
        int count = 0;
        for (Object object : collection) {
          buildFlattenedMap(result,
              Collections.singletonMap("[" + (count++) + "]", object), key);
        }
      }
      else {
        result.put(key, value == null ? "" : value);
      }
    }
  }


  /**
   * Callback interface used to process properties in a resulting map.
   */
  public interface MatchCallback {

    /**
     * Process the properties.
     * @param properties the properties to process
     * @param map a mutable result map
     */
    void process(Properties properties, Map<String, Object> map);
  }


  /**
   * Strategy interface used to test if properties match.
   */
  public interface DocumentMatcher {

    /**
     * Test if the given properties match.
     * @param properties the properties to test
     * @return the status of the match.
     */
    MatchStatus matches(Properties properties);
  }


  /**
   * Status returned from {@link DocumentMatcher#matches(java.util.Properties)}
   */
  public enum MatchStatus {

    /**
     * A match was found.
     */
    FOUND,

    /**
     * No match was found.
     */
    NOT_FOUND,

    /**
     * The matcher should not be considered.
     */
    ABSTAIN;

    /**
     * Compare two {@link MatchStatus} items, returning the most specific status.
     */
    public static MatchStatus getMostSpecific(MatchStatus a, MatchStatus b) {
      return (a.ordinal() < b.ordinal() ? a : b);
    }
  }


  /**
   * Method to use for resolving URIs.
   */
  public enum ResolutionMethod {

    /**
     * Replace values from earlier in the list.
     */
    OVERRIDE,

    /**
     * Replace values from earlier in the list, ignoring any failures.
     */
    OVERRIDE_AND_IGNORE,

    /**
     * Take the first URI in the list that exists and use just that.
     */
    FIRST_FOUND
  }


  /**
   * A specialized {@link Constructor} that checks for duplicate keys.
   */
  protected static class StrictMapAppenderConstructor extends Constructor {

    public  StrictMapAppenderConstructor() {
      super();
    }

    @Override
    protected Map<Object, Object> constructMapping(MappingNode node) {
      try {
        return super.constructMapping(node);
      } catch (IllegalStateException e) {
        throw new ParserException("while parsing MappingNode",
            node.getStartMark(), e.getMessage(), node.getEndMark());
      }
    }
    private static class DefaultMap extends AbstractMap<Object, Object> {
      private final Map<Object, Object> delegate;
      public DefaultMap(Map<Object, Object> delegate) {
        this.delegate = delegate;
      }
      @Override
      public Object put(Object key, Object value) {
        if (delegate.containsKey(key)) {
          throw new IllegalStateException("duplicate key: " + key);
        }
        return delegate.put(key, value);
      }
      @Override
      public Set<Entry<Object, Object>> entrySet() {
        return delegate.entrySet();
      }
    }
    @Override
    protected Map<Object, Object> createDefaultMap() {
      final Map<Object, Object> delegate = super.createDefaultMap();
      return new DefaultMap(delegate);
    }
  }
  private static class PutAllMatchCallback implements MatchCallback {
    private final Properties properties;
    public PutAllMatchCallback(Properties properties) {
      this.properties = properties;
    }
    @Override
    public void process(Properties properties, Map<String, Object> map) {
      this.properties.putAll(properties);
    }
  }
  protected Properties createProperties() {
    final Properties result = new Properties();
    process(new PutAllMatchCallback(result));
    return result;
  }

}
