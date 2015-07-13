package com.nitorcreations.willow.eventhandler;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.beanutils.BeanUtils;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.nitorcreations.willow.utils.MergeableProperties;
import com.nitorcreations.willow.utils.SimpleFormatter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Main class for event handler.
 * 
 * @author mtommila
 */
@Named
@Singleton
@SuppressFBWarnings(value={"DM_EXIT"}, justification="cli tool needs to convey correct exit code")
public class Main {

  protected Logger logger = Logger.getLogger(getClass().getName());

  @Inject
  protected Injector injector;

  protected MergeableProperties properties;

  @Inject
  protected ExecutorService executorService;

  @Inject
  protected EventPoller eventPoller;

  /**
   * Command-line entry point.
   * 
   * @param args The command line arguments. These should be URLs that point to the configuration file(s).
   */
  public static void main(String... args) {
    main(new Main(), args);
  }

  /**
   * Subclass specific entry point.
   * 
   * @param main The actual main class implementation to use.
   * @param args The command line arguments.
   */
  protected static void main(Main main, String... args) {
    main.getInjector().getInstance(main.getMainClass()).doMain(args);
  }

  /**
   * Get an Injector to be used in instantiating the main class.
   * 
   * @return An Injector.
   */
  protected Injector getInjector() {
    return Guice.createInjector(getModules());
  }

  /**
   * Get the Guice modules to be used in injecting dependencies.
   * 
   * @return The modules.
   */
  protected Module[] getModules() {
    Module[] modules = { new WireModule(new EventHandlerModule(),
        new SpaceModule(new URLClassSpace(Main.class.getClassLoader()))) };
    return modules;
  }

  /**
   * Get the main class to be instantiated and run.
   * 
   * @return The main class to be used.
   */
  protected Class<? extends Main> getMainClass() {
    return getClass();
  }

  /**
   * Actual main implementation.
   * 
   * @param args The command line arguments. These should be URLs that point to the configuration file(s).
   */
  protected void doMain(String... args) {
    properties = new MergeableProperties();
    properties.putAll(System.getProperties());
    for (String arg : args) {
      properties.merge(arg);
    }
    // Read event handling configuration
    Map<String, List<EventHandler>> eventHandlers = new LinkedHashMap<>();
    MergeableProperties allProperties = properties.getPrefixed("willow-event-handler.handlers");
    int i = 0;
    while (true) {
      MergeableProperties handlerProperties = allProperties.getPrefixed("[" + i++ + "]");
      if (handlerProperties.isEmpty()) {
        break;
      }
      String handlerClassName = handlerProperties.getProperty("handler");
      String eventClassName = handlerProperties.getProperty("event");
      try {
        EventHandler eventHandler = (EventHandler) injector.getInstance(Class.forName(handlerClassName));
        MergeableProperties beanProperties = handlerProperties.getPrefixed("properties");
        for (String propertyName : beanProperties.stringPropertyNames()) {
          String propertyValue = beanProperties.getProperty(propertyName);
          BeanUtils.setProperty(eventHandler, propertyName, propertyValue);
        }
        List<EventHandler> eventHandlersForType = eventHandlers.get(eventClassName);
        if (eventHandlersForType == null) {
          eventHandlersForType = new ArrayList<>();
          eventHandlers.put(eventClassName, eventHandlersForType);
        }
        eventHandlersForType.add(eventHandler);
      } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException e) {
        logger.log(Level.SEVERE, "Error instantiating handler", e);
        System.exit(1);
      }
    }

    if (eventHandlers .isEmpty()) {
      logger.info("No event handlers configured. Exiting.");
      System.exit(0);
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        Main.this.stop();
      }
    });

    // Start polling events
    String uri = properties.getProperty("willow-event-handler.eventsUri");
    try {
      logger.info("Initializing event poller");
      eventPoller.initialize(eventHandlers, new URI(uri));
    } catch (URISyntaxException e) {
      logger.log(Level.SEVERE, "Invalid URI: " + uri, e);
      System.exit(1);
    }
  }

  protected void stop() {
    System.out.println("Willow event handler stopping...");
    executorService.shutdown();
    System.out.println("Willow event handler stopped.");
  }

  protected void setupLogging() {
    Logger rootLogger = Logger.getLogger("");
    for (Handler nextHandler : rootLogger.getHandlers()) {
      rootLogger.removeHandler(nextHandler);
    }
    Handler console = new ConsoleHandler();
    console.setLevel(Level.INFO);
    console.setFormatter(new SimpleFormatter());
    rootLogger.addHandler(console);
    rootLogger.setLevel(Level.INFO);
    console.setFilter(new Filter() {
      @Override
      public boolean isLoggable(LogRecord record) {
        return record.getLoggerName() == null || !record.getLoggerName().startsWith("org.eclipse.jetty.util.log");
      }
    });
  }
}
