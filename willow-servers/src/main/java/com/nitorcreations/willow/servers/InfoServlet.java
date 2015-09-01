package com.nitorcreations.willow.servers;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
public class InfoServlet extends HttpServlet {
  private static final long serialVersionUID = -1042463048053284271L;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    response.setContentType("text/html;charset=UTF-8");
    PrintWriter out = response.getWriter();
    try {
      out.println("<!DOCTYPE html>");
      out.println("<html><head>");
      out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
      String title = "Request Info";
      out.println("<head><title>" + title + "</title></head>");
      out.println("<body>");
      out.println("<h3>" + title + "</h3>");
      out.println("<table>");
      out.println("<tr><td>Protocol</td>");
      out.println("<td>" + request.getProtocol() + "</td></tr>");
      out.println("<tr><td>Method</td>");
      out.println("<td>" + request.getMethod() + "</td></tr>");
      out.println("</td></tr><tr><td>");
      out.println("<tr><td>URI</td>");
      out.println("<td>" + filter(request.getRequestURI()) + "</td></tr>");
      out.println("<tr><td>Path Info</td>");
      out.println("<td>" + filter(request.getPathInfo()) + "</td></tr>");
      out.println("<tr><td>Path Translated:</td>");
      out.println("<td>" + request.getPathTranslated() + "</td></tr>");
      out.println("<tr><td>Remote address</td>");
      out.println("<td>" + request.getRemoteAddr() + "</td></tr>");
      String cipherSuite =  (String)request.getAttribute("javax.servlet.request.cipher_suite");
      if (cipherSuite != null) {
        out.println("<tr><td>SSLCipherSuite:</td>");
        out.println("<td>" + cipherSuite + "</td></tr>");
      }
      out.println("</table></body></html>");
    } finally {
      out.close();
    }
  }
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    doGet(request, response);
  }
  private static String filter(String message) {
    if (message == null) return null;
    int len = message.length();
    StringBuffer result = new StringBuffer(len + 20);
    char aChar;

    for (int i = 0; i < len; ++i) {
      aChar = message.charAt(i);
      switch (aChar) {
      case '<': result.append("&lt;"); break;
      case '>': result.append("&gt;"); break;
      case '&': result.append("&amp;"); break;
      case '"': result.append("&quot;"); break;
      default:  result.append(aChar);
      }
    }
    return result.toString();
  }
}
