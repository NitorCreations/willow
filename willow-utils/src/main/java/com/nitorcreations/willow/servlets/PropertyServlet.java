package com.nitorcreations.willow.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.nitorcreations.willow.utils.MergeableProperties;
import com.nitorcreations.willow.utils.PropertySource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class PropertyServlet extends HttpServlet {
  private static final long serialVersionUID = -7870102334063578984L;
  ServletConfig config;
  private transient PropertySource propertySource;
  private String obfuscatedPrefix = "obfuscated:";
  @SuppressFBWarnings(value={"SE_TRANSIENT_FIELD_NOT_RESTORED"}, justification="Logger always initialized freshly")
  private transient Logger log = Logger.getLogger(PropertyServlet.class.getName());
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
        log.info("PropertySource class " + propertySourceClassName + " not found");
      } catch (InstantiationException | IllegalAccessException e) {
        log.log(Level.INFO, "Unable to instantiate PropertySource class " + propertySourceClassName, e);
      }
    }
  }

  @Override
  public ServletConfig getServletConfig() {
    return config;
  }

  @Override
  @SuppressFBWarnings(value={"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"},
      justification="null check in try-with-resources magic bytecode")
  public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (!req.getMethod().equals("GET")) {
      res.sendError(405, "Only GET allowed");
      return;
    }
    String path = req.getPathInfo();
    if (path == null) {
      path = req.getServletPath();
    }
    String rootProperties = path.substring(1);
    MergeableProperties mrg = null;
    if (config.getInitParameter("property.roots") != null) {
      String roots = config.getInitParameter("property.roots");
      if (!roots.isEmpty()) {
        mrg = new MergeableProperties(roots.split("\\|"));
      } else {
        mrg = new MergeableProperties("classpath:properties/");
      }
    } else {
      mrg = new MergeableProperties("classpath:properties/");
    }
    MergeableProperties seed = new MergeableProperties();
    for (Entry<String, String[]> next : req.getParameterMap().entrySet()) {
      seed.setProperty(next.getKey(), StringUtils.join(next.getValue(), ","), false);
    }
    Enumeration<String> it = req.getHeaderNames();
    while (it.hasMoreElements()) {
      String key = it.nextElement();
      String value = req.getHeader(key);
      seed.setProperty(key.toLowerCase(Locale.ENGLISH), value, false);
    }
    ServletContext ctx = getServletContext();
    seed.setProperty("path", path, false);
    String context = ctx.getContextPath() == null ? "" : ctx.getContextPath();
    seed.setProperty("context", context, false);
    res.setContentType("text/plain;charset=utf-8");
    res.setStatus(200);
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
