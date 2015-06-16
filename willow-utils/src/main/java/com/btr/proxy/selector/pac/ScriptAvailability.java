package com.btr.proxy.selector.pac;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/****************************************************************************
 * Utility to check availablility of javax.script
 *
 * @author Bernd Rosstauscher (proxyvole@rosstauscher.de) Copyright 2009
 ***************************************************************************/
abstract class ScriptAvailability {

  /*************************************************************************
   * Checks whether javax.script is available or not. Completely done per
   * Reflection to allow compilation under Java 1.5
   *
   * @return true if javax.script is available; false otherwise
   ************************************************************************/
  public static boolean isJavaxScriptingAvailable() {
    Object engine = null;
    try {
      Class<?> managerClass = Class.forName("javax.script.ScriptEngineManager");
      Method m = managerClass.getMethod("getEngineByMimeType", String.class);
      engine = m.invoke(managerClass.newInstance(), "text/javascript");
    } catch (ClassNotFoundException | NoSuchMethodException
        | IllegalAccessException | InvocationTargetException
        | InstantiationException e) {
      return false;
    }

    return engine != null;
  }

  /*************************************************************************
   * Constructor
   ************************************************************************/

  ScriptAvailability() {
    super();
  }
}
