package com.nitorcreations.logging.jetty;

import java.net.URISyntaxException;

import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

import com.nitorcreations.logging.WillowAccessLogHelper;

@ManagedObject("NCSA standard format request log")
public class WebsocketRequestLog extends AbstractLifeCycle implements
    RequestLog {
  private final WillowAccessLogHelper transmitter;
  private transient PathMap<String> _ignorePathMap = new PathMap<String>();
  private boolean _preferProxiedForAddress = true;

  public WebsocketRequestLog(long flushInterval, String url)
      throws URISyntaxException {
    super();
    this.transmitter = new WillowAccessLogHelper(flushInterval, url);
  }

  @Override
  public void log(Request request, int status, long written) {
    if (_ignorePathMap != null
        && _ignorePathMap.getMatch(request.getRequestURI()) != null)
      return;
    transmitter.queue(new AccessLogJettyAdapter(request, status, written,
        _preferProxiedForAddress));
  }

  public void setPreferProxiedForAddress(boolean b) {
    this._preferProxiedForAddress = b;
  }

}
