package com.nitorcreations.willow.utils;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FilePollingProperties {
  public interface PropertyChangeListerner {
    public void propertyValueChanged(String key, String newValue, String oldValue);
    public void propertyAdded(String key, String value);
    public void propertyRemoved(String key, String value);
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
  private final MergeableProperties properties;

  public FilePollingProperties(final File source) {
    this(source, new LoggingPropertyChangeListener(FilePollingProperties.class.getName(), Level.FINE));
  }
  public FilePollingProperties(final File source, final PropertyChangeListerner listener) {
    properties = new MergeableProperties();
    try {
      properties.load(new FileInputStream(source));
    } catch (IOException e) {
      //Noop
    }
    final Path sourcePath = source.getAbsoluteFile().getParentFile().toPath();
    try {
      final WatchService watcher = FileSystems.getDefault().newWatchService();
      sourcePath.register(watcher, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
      new Thread(new Runnable() {
        @Override
        public void run() {
          for (;;) {
            WatchKey key = null;
            try {
              key = watcher.take();
            } catch (InterruptedException x) {
              return;
            }
            for (WatchEvent<?> event: key.pollEvents()) {
              WatchEvent.Kind<?> kind = event.kind();
              if (kind == OVERFLOW) {
                  continue;
              }
              @SuppressWarnings("unchecked")
              WatchEvent<Path> ev = (WatchEvent<Path>)event;
              Path context = ev.context();
              if (!context.equals(source.toPath())) {
                continue;
              }
              if (kind == ENTRY_DELETE) {
                for (Entry<String, String> next : properties.backingEntrySet()) {
                  listener.propertyRemoved(next.getKey(), next.getValue());
                }
                properties.clear();
              } else {
                MergeableProperties newProps = new MergeableProperties();
                try {
                  newProps.load(new FileInputStream(source));
                  MergeableProperties oldProps ;
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
                }
              }
            }
            boolean valid = key.reset();
            if (!valid) {
              break;
            }
            continue;
          }
        }
      }).start();
    } catch (Exception e) {
    }
  }
  public MergeableProperties getProperties() {
    synchronized (properties) {
      return (MergeableProperties) properties.clone();
    }
  }
  public static void main(String[] args) throws IOException {
    new FilePollingProperties(new File(args[0]), new LoggingPropertyChangeListener("main", Level.INFO));
  }
}
