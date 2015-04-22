package com.nitorcreations.willow.servers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.JdkLogChute;

public class VelocityServlet extends HttpServlet {

  private static final long serialVersionUID = 5202740307439072493L;
  private String encoding;
  private String contentType;
  private String configLocation;
  private String requestKey;
  private String sessionKey;
  private String applicationKey;
  private String systemKey;
  private String xmlKey;
  private VelocityEngine engine;

  @Override
  public void init() throws ServletException {
    this.encoding = "UTF-8";
    this.contentType = "text/html";
    this.configLocation = "/WEB-INF/velocity.properties";
    this.requestKey = "request";
    this.sessionKey = "session";
    this.applicationKey = "application";
    this.xmlKey = "xml";
    this.systemKey = "system";
    this.engine = createEngine();
  }

  private VelocityEngine createEngine() throws ServletException {
    final VelocityEngine velocity = new VelocityEngine();

    velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
      JdkLogChute.class.getName());
    velocity.setProperty(RuntimeConstants.RESOURCE_LOADER,
      WebappResourceLoader.NAME);
    velocity.setProperty(WebappResourceLoader.NAME + '.'
      + RuntimeConstants.RESOURCE_LOADER + ".class",
      WebappResourceLoader.class.getName());
    velocity.setApplicationAttribute(ServletContext.class.getName(),
      getServletContext());

    velocity.init(loadProperties());

    return velocity;
  }

  private Properties loadProperties() throws ServletException {
    final InputStream resource = getServletContext().getResourceAsStream(
      this.configLocation);
    if (resource == null) {
      return new Properties();
    }
    try {
      final Properties props = new Properties();
      props.load(resource);
      return props;
    } catch (IOException e) {
      throw new ServletException(e);
    } finally {
      try {
        resource.close();
      } catch (IOException e) {
        throw new ServletException(e);
      }
    }
  }

  @Override
  protected void doGet(final HttpServletRequest request,
    final HttpServletResponse response) throws ServletException,
    IOException {
    render(request, response);
  }

  @Override
  protected void doPost(final HttpServletRequest request,
    final HttpServletResponse response) throws ServletException,
    IOException {
    render(request, response);
  }

  private void render(final HttpServletRequest request,
    final HttpServletResponse response) throws ServletException, IOException {

    String templ = request.getServletPath();
    if (templ.isEmpty()) {
      templ = new URL(request.getRequestURL().toString()).getPath() + request.getRequestURI();
    }
    final Template template = this.engine.getTemplate(templ, this.encoding);
    final VelocityContext context = new VelocityContext();
    context.put(this.requestKey, toMap(request));
    context.put(this.sessionKey, toMap(request.getSession(false)));
    context.put(this.applicationKey, toMap(getServletContext()));
    context.put(this.xmlKey, XMLTool.class);
    context.put(this.systemKey, System.class);
    response.setContentType(this.contentType);
    response.setCharacterEncoding(this.encoding);
    try {
      template.merge(context, response.getWriter());
    } catch (final ResourceNotFoundException e) {
      throw new ServletException(e);
    } catch (final ParseErrorException e) {
      throw new ServletException(e);
    } catch (final MethodInvocationException e) {
      throw new ServletException(e);
    }
  }

  private Map<String, Object> toMap(final HttpServletRequest request) {
    final Map<String, Object> map = newMap();
    final Enumeration<String> names = request.getAttributeNames();
    while (names.hasMoreElements()) {
      final String name = names.nextElement();
      map.put(name, request.getAttribute(name));
    }
    return Collections.unmodifiableMap(map);
  }
  private Map<String, Object> toMap(final HttpSession session) {
    if (session == null) {
      return Collections.emptyMap();
    }
    final Map<String, Object> map = newMap();
    final Enumeration<String> names = session.getAttributeNames();
    while (names.hasMoreElements()) {
      final String name = names.nextElement();
      map.put(name, session.getAttribute(name));
    }
    return Collections.unmodifiableMap(map);
  }
  private Map<String, Object> toMap(final ServletContext context) {
    final Map<String, Object> map = newMap();
    final Enumeration<String> names = context.getAttributeNames();
    while (names.hasMoreElements()) {
      final String name = names.nextElement();
      map.put(name, context.getAttribute(name));
    }
    return Collections.unmodifiableMap(map);
  }
  private LinkedHashMap<String, Object> newMap() {
    return new LinkedHashMap<String, Object>();
  }
}
