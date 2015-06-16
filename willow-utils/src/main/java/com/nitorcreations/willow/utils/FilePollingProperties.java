package com.nitorcreations.willow.utils;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.nitorcreations.willow.utils.PollingFile.FileListener;

public class FilePollingProperties implements FileListener {
  private Logger logger = Logger.getLogger(getClass().getCanonicalName());
  public interface PropertyChangeListerner {
    void propertyValueChanged(String key, String newValue, String oldValue);
    void propertyAdded(String key, String value);
    void propertyRemoved(String key, String value);
  }
  public static class LoggingPropertyChangeListener implements PropertyChangeListerner {
    private final Logger log;
    private final Level level;
    public LoggingPropertyChangeListener(String loggerName, Level level) {
      this.log = Logger.getLogger(loggerName);
      this.level = level;
    }

    @Override
    public void propertyValueChanged(String key, String newValue, String oldValue) {
      log.log(level, "CHANGE: " + key + " = " + oldValue + " => " + newValue);
    }

    @Override
    public void propertyRemoved(String key, String value) {
      log.log(level, "REMOVED: " + key + " = " + value);
    }

    @Override
    public void propertyAdded(String key, String value) {
      log.log(level, "ADDED: " + key + " = " + value);
    }
  }
  private final PropertyChangeListerner listener;
  private final MergeableProperties properties;

  public FilePollingProperties(final String source) {
    this(source, new LoggingPropertyChangeListener(FilePollingProperties.class.getName(), Level.FINE));
  }
  public FilePollingProperties(final String source, final PropertyChangeListerner listener) {
    properties = new MergeableProperties();
    this.listener = listener;
    try (FileInputStream in = new FileInputStream(source)) {
      properties.load(in);
    } catch (IOException e) {
      logger.log(Level.FINER, "Exception while polling for changes", e);
    }
    PollingFile backend = new PollingFile(source, this);
    backend.startListening();
  }

  public MergeableProperties getProperties() {
    synchronized (properties) {
      return (MergeableProperties) properties.clone();
    }
  }
  public static void main(String[] args) throws IOException {
    new FilePollingProperties(args[0], new LoggingPropertyChangeListener("main", Level.INFO));
  }

  @Override
  public void fileChanged(File file, Kind<Path> kind) {
    MergeableProperties oldProps ;
    if (kind == ENTRY_DELETE) {
      oldProps = (MergeableProperties) properties.clone();
      properties.clear();
      for (Entry<String, String> next : oldProps.backingEntrySet()) {
        listener.propertyRemoved(next.getKey(), next.getValue());
      }
    } else {
      MergeableProperties newProps = new MergeableProperties();
      try (FileInputStream in = new FileInputStream(file)) {
        newProps.load(in);
        synchronized (properties) {
          oldProps = (MergeableProperties) properties.clone();
          properties.clear();
          properties.putAll(newProps);
        }
        for (Entry<String, String> next : newProps.backingEntrySet()) {
          if (oldProps.containsKey(next.getKey())) {
            if (!oldProps.getProperty(next.getKey()).equals(next.getValue())) {
              listener.propertyValueChanged(next.getKey(), next.getValue(), oldProps.getProperty(next.getKey()));
            }
            oldProps.remove(next.getKey());
          } else {
            listener.propertyAdded(next.getKey(), next.getValue());
          }
        }
        for (Entry<String, String> next : oldProps.backingEntrySet()) {
          listener.propertyRemoved(next.getKey(), next.getValue());
        }
      } catch (IOException e) {
        logger.fine("Failed to load new properties");
      }
    }
  }
}
