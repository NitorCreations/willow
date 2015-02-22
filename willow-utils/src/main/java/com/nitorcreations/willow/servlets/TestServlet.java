package com.nitorcreations.willow.servlets;

import java.io.IOException;
import java.util.Random;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

public class TestServlet extends HttpServlet {
  private static final long serialVersionUID = 6630375757617553307L;
  int min = 5;
  int max = 45000;

  @Override
  public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
    String[] wait = req.getParameterValues("wait");
    if (wait == null)
      wait = new String[] { randomWait() };
    try {
      Thread.sleep(Integer.valueOf(wait[0]));
      res.setContentType("text/plain");
      res.getOutputStream().write("OK".getBytes(), 0, 2);
      res.getOutputStream().close();
    } catch (NumberFormatException | InterruptedException e) {
      throw new ServletException(e);
    }
  }

  private String randomWait() {
    Random random = new Random();
    int randomNumber = random.nextInt(max - min) + min;
    return Integer.toString(randomNumber);
  }
}
