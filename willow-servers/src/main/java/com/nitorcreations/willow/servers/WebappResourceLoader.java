package com.nitorcreations.willow.servers;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletContext;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

public class WebappResourceLoader extends ResourceLoader {

  public static final String NAME = "webapp";

  private ServletContext servletContext;

  @Override
  public void init(ExtendedProperties configuration) {
    final String scClassName = ServletContext.class.getName();
    final Object sc = this.rsvc.getApplicationAttribute(scClassName);
    if (sc instanceof ServletContext) {
      servletContext = (ServletContext) sc;
    } else {
      log.error("Not found " + scClassName + ": " + sc);
    }
  }

  @Override
  public InputStream getResourceStream(final String source) throws ResourceNotFoundException {

    if (source == null || source.length() == 0) {
      throw new ResourceNotFoundException("No template name");
    }

    final InputStream res = servletContext.getResourceAsStream(source);
    if (res == null) {
      throw new ResourceNotFoundException(source);
    }
    return res;
  }

  @Override
  public boolean isSourceModified(Resource resource) {
    try {
      URL url = servletContext.getResource(resource.getName());
      if (url == null) return true;
      if (url.toString().startsWith("file:")) {
        return urlToFile(url).lastModified() > resource.getLastModified();
      } else if (url.toString().startsWith("jar:")) {
        String sUrl = url.toString();
        String fName = sUrl.split("\\!")[0].substring(4); 
        return urlToFile(new URL(fName)).lastModified() > resource.getLastModified();
      } else {
        return true;
      }
    } catch (MalformedURLException e) {
      return true;
    }
  }
  private File urlToFile(URL url) {
    File f;
    try {
      f = new File(url.toURI());
    } catch(URISyntaxException e) {
      f = new File(url.getPath());
    }
    return f;
  }
  @Override
  public long getLastModified(Resource resource) {
    long now = System.currentTimeMillis();
    try {
      URL url = servletContext.getResource(resource.getName());
      if (url == null) return now;
      if (url.toString().startsWith("file:")) {
        return urlToFile(url).lastModified();
      } else if (url.toString().startsWith("jar:")) {
        String sUrl = url.toString();
        String fName = sUrl.split("\\!")[0].substring(4); 
        return urlToFile(new URL(fName)).lastModified();
      } else {
        return now;
      }
    } catch (MalformedURLException | NullPointerException e) {
      return now;
    }
  }
}
