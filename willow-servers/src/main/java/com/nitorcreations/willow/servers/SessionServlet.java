package com.nitorcreations.willow.servers;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.google.gson.Gson;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Singleton
@SuppressFBWarnings(value={"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
  justification="Fields encoded into JSON and thusly used")
public class SessionServlet extends HttpServlet {
  private static final long serialVersionUID = -8042909270647592309L;
  public static class SessionInfo {
    public String username;
    public boolean isAdmin = false;
    public boolean isMonitor = false;
  }
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    res.setContentType("application/javascript");
    SessionInfo ret = new SessionInfo();
    Subject sub = SecurityUtils.getSubject();
    ret.username = sub.getPrincipal().toString();
    ret.isAdmin = sub.isPermitted("admin");
    ret.isMonitor = sub.isPermitted("monitor");
    String retStr =  "var session=" + new Gson().toJson(ret)  + ";";
    res.getWriter().write(retStr);
  }
}
