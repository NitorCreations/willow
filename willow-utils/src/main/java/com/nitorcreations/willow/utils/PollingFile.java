package com.nitorcreations.willow.utils;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;

public class PollingFile extends File implements Runnable {
  private final AtomicBoolean running = new AtomicBoolean(true);
  private transient Thread poller = null;
  
  public interface FileListener {
    public void fileChanged(File file, WatchEvent.Kind<Path> kind);
  }
  private FileListener listener;

  public PollingFile(File parent, String child, FileListener listener) {
    super(parent, child);
    this.listener = listener;
  }
  public PollingFile(String name, FileListener listener) {
    super(name);
    this.listener = listener;
  }
  public PollingFile(String parent, String child, FileListener listener) {
    super(parent, child);
    this.listener = listener;
  }
  public PollingFile(URI uri, FileListener listener) {
    super(uri);
    this.listener = listener;
  }
  public void startListening() {
    poller = new Thread(this, getAbsolutePath() + "-listener");
    poller.start();
  }
  private static final long serialVersionUID = -6683198740670677087L;

  @Override
  public void run() {
    try {
      WatchService watcher = FileSystems.getDefault().newWatchService();
      getAbsoluteFile().getParentFile().toPath().register(watcher, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
      while (running.get()) {
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
          if (context.equals(toPath())) {
            listener.fileChanged(this, ev.kind());
          }
        }
        boolean valid = key.reset();
        if (!valid) {
          break;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to watch file changes", e);
    }
  }
  public void stop() {
    running.set(false);
    if (poller != null) {
      poller.interrupt();
    }
  }
  @Override
  public int hashCode() {
    return super.hashCode();
  }
  @Override
  public boolean equals(Object obj) {
    if ((obj != null) && obj.getClass().isAssignableFrom(PollingFile.class)) {
      return compareTo((File)obj) == 0;
    }
    return false;
  }
}
