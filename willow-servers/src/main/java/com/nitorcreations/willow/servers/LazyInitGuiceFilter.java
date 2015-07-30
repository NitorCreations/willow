package com.nitorcreations.willow.servers;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


import com.google.inject.servlet.GuiceFilter;

public class LazyInitGuiceFilter implements Filter {
  private FilterConfig filterconfig;
  private GuiceFilter delegate;
  private final WillowServletContextListener listener;
  
  public LazyInitGuiceFilter(WillowServletContextListener listener) {
    this.listener = listener;
  }
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterconfig = filterConfig;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    getGuiceFilter().doFilter(request, response, chain);
  }

  @Override
  public void destroy() {
    try {
      getGuiceFilter().destroy();
    } catch (ServletException e) {
      Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Failed to destroy gucefilter", e);
    }
  }
  
  private GuiceFilter getGuiceFilter() throws ServletException {
    if (delegate == null) {
      delegate = listener.getInjector().getInstance(GuiceFilter.class);
      delegate.init(filterconfig);
    }
    return delegate;
  }
}
