package com.nitorcreations.willow.servers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
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
  private String encoding = "UTF-8";
  private String contentType = "text/html";
  private String configLocation = "velocity.properties";
  private String requestKey = "request";
  private String sessionKey = "session";
  private String applicationKey = "application";
  private String systemKey = "system";
  private String xmlKey = "xml";
  private String subjectKey = "subject";
  private transient VelocityEngine engine;

  @Override
  public void init() throws ServletException {
    this.engine = createEngine();
  }

  private VelocityEngine createEngine() throws ServletException {
    final VelocityEngine velocity = new VelocityEngine();

    velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, JdkLogChute.class.getName());
    velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, WebappResourceLoader.NAME);
    velocity.setProperty(WebappResourceLoader.NAME + '.' + RuntimeConstants.RESOURCE_LOADER + ".class",
      WebappResourceLoader.class.getName());
    velocity.setApplicationAttribute(ServletContext.class.getName(), getServletContext());
    velocity.init(loadProperties());
    return velocity;
  }

  private Properties loadProperties() throws ServletException {
    try (InputStream resource = getClass().getClassLoader().getResourceAsStream(
      this.configLocation)) {
      if (resource == null) {
        return new Properties();
      }
      final Properties props = new Properties();
      props.load(resource);
      return props;
    } catch (IOException e) {
      throw new ServletException(e);
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
    context.put(this.requestKey, request);
    context.put(this.sessionKey, request.getSession(true));
    context.put(this.applicationKey, getServletContext());
    context.put(this.xmlKey, XMLTool.class);
    context.put(this.systemKey, System.class);
    context.put(this.subjectKey, SecurityUtils.getSubject());
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
  private LinkedHashMap<String, Object> newMap() {
    return new LinkedHashMap<String, Object>();
  }
}
