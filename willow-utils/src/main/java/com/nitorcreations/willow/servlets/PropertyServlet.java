package com.nitorcreations.willow.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.nitorcreations.willow.utils.MergeableProperties;
import com.nitorcreations.willow.utils.PropertySource;

public class PropertyServlet extends HttpServlet {
  private static final long serialVersionUID = -7870102334063578984L;
  ServletConfig config;
  PropertySource propertySource;
  String obfuscatedPrefix = "obfuscated:";

  @Override
  public void init(ServletConfig config) throws ServletException {
    this.config = config;
    String propertySourceParameter = config.getInitParameter("propertysource.systemproperty");
    if (propertySourceParameter == null) {
      propertySourceParameter = "com.nitocreations.willow.PROPERTY_SOURCE";
    }
    if (config.getInitParameter("obfuscated.prefix") != null) {
      obfuscatedPrefix = config.getInitParameter("obfuscated.prefix");
    }
    String propertySourceClassName = System.getProperty(propertySourceParameter);
    if (propertySourceClassName != null) {
      try {
        @SuppressWarnings("unchecked")
        Class<? extends PropertySource> propertySourceClass = (Class<? extends PropertySource>) Class.forName(propertySourceClassName);
        propertySource = propertySourceClass.newInstance();
      } catch (ClassNotFoundException e) {
        log("PropertySource class " + propertySourceClassName + " not found");
      } catch (InstantiationException | IllegalAccessException e) {
        log("Unable to instantiate PropertySource class " + propertySourceClassName, e);
      }
    }
  }

  @Override
  public ServletConfig getServletConfig() {
    return config;
  }

  @Override
  public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
    if (!((HttpServletRequest) req).getMethod().equals("GET")) {
      ((HttpServletResponse) res).sendError(405, "Only GET allowed");
      return;
    }
    String path = ((HttpServletRequest) req).getPathInfo();
    if (path == null) {
      path = ((HttpServletRequest) req).getServletPath();
    }
	String rootProperties = path.substring(1);
    MergeableProperties mrg = null;
    if (config.getInitParameter("property.roots") != null) {
      String roots = config.getInitParameter("property.roots");
      if (!roots.isEmpty()) {
        mrg = new MergeableProperties(roots.split("\\|"));
      } else {
        mrg = new MergeableProperties();
      }
    } else {
      mrg = new MergeableProperties();
    }
    MergeableProperties seed = new MergeableProperties();
    for (Entry<String, String[]> next : ((HttpServletRequest) req).getParameterMap().entrySet()) {
      seed.setProperty(next.getKey(), StringUtils.join(next.getValue(), ","), false);
    }
    Enumeration<String> it = ((HttpServletRequest) req).getHeaderNames();
    while (it.hasMoreElements()) {
      String key = it.nextElement();
      String value = ((HttpServletRequest) req).getHeader(key);
      seed.setProperty(key.toLowerCase(), value, false);
    }
    ServletContext ctx = getServletContext();
    seed.setProperty("path", path, false);
    seed.setProperty("context", ctx.getContextPath(), false);
    res.setContentType("text/plain;charset=utf-8");
    ((HttpServletResponse) res).setStatus(200);
    mrg.merge(seed, rootProperties);
    if (propertySource != null) {
      mrg.deObfuscate(propertySource, obfuscatedPrefix);
    }
    try (OutputStream out = res.getOutputStream()) {
      mrg.store(out, null);
      out.flush();
    }
  }

  @Override
  public String getServletInfo() {
    return "PropertiesServlet";
  }

  @Override
  public void destroy() {}
}
