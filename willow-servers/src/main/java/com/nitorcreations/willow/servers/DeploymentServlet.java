package com.nitorcreations.willow.servers;

import java.awt.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.DefaultServlet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import com.nitorcreations.willow.download.StreamPumper;
import com.nitorcreations.willow.servlets.PropertyServlet;
import com.nitorcreations.willow.utils.EnumerationIterable;

import at.spardat.xma.xdelta.JarPatcher;

public class DeploymentServlet extends DefaultServlet {
  
  File root;
  private static final long serialVersionUID = 4507192371045140774L;
  private ServletConfig config;
  @Override
  public void init(final ServletConfig config) throws ServletException {
    String rootPath = config.getInitParameter("deployment.data.root");
    if (rootPath != null) {
      root = new File(rootPath).getAbsoluteFile();
    } else {
      root = new File(new File(".").getAbsoluteFile(), "deploydata");
    }
    this.config = config;
    ServletConfig delegateConfig;
    try {
      delegateConfig = new DelegateServletConfig(config, root);
    } catch (MalformedURLException e) {
      throw new ServletException("Failed to initilaize deployment servlet", e);
    }
    super.init(delegateConfig);
  }
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    String[] path = req.getPathInfo().split("/");
    if (path.length < 3 || !new File(root, path[1]).exists() || !new File(root, path[1]).isDirectory()) {
      resp.sendError(404);
      return;
    }
    if (path[2].equals("diffcandidates")) {
      ArrayList<String> ret = new ArrayList<>();
      File[] versions = new File(root, path[1]).listFiles();
      if (versions != null) {
        Arrays.sort(versions, new Comparator<File>() {
          @Override
          public int compare(File o1, File o2) {
            return (int)(o1.lastModified() - o2.lastModified());
          }
        });
        for (File next : versions) {
          ret.add(next.getName());
        }
      }
      try (OutputStream out = resp.getOutputStream()) {
        resp.setStatus(200);
        new Gson().toJson(ret, TypeToken.get(List.class).getType(), 
            new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
        return;
      }
    }
    if (!new File(new File(root, path[1]), path[2]).exists()) {
      resp.sendError(404);
      return;
    }
    if (path.length == 3 || path[3].isEmpty()) {
      File pkg = new File(new File(new File(root, path[1]), path[2]), ".systempkg");
      if (!pkg.exists()) {
        resp.sendError(404);
        return;
      }
      resp.setStatus(200);
      resp.setContentType("application/zip");
      try (FileInputStream in = new FileInputStream(pkg);
          OutputStream out = resp.getOutputStream()) {
        new StreamPumper(in, out).run();
      }
      return;
    }
    if (path[3].equals("properties")) {
      File pkg = new File(new File(new File(root, path[1]), path[2]), "properties");
      PropertyServlet propertyServlet = new PropertyServlet();
      propertyServlet.init(new DelegateServletConfig(config, root, pkg));
      propertyServlet.service(req, resp);
      return;
    }
    super.doGet(req, resp);
  }
  @Override
  @SuppressWarnings("PMD.UselessParentheses")
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
      throws ServletException, IOException {
    String[] path = req.getPathInfo().split("/");
    if (path.length < 3 || !new File(root, path[1]).exists() || !new File(root, path[1]).isDirectory()
        || path.length > 4 || (path.length > 3 && !path[4].isEmpty())) {
      resp.sendError(404);
      return;
    }
    File target = new File(new File(new File(root, path[1]), path[2]), ".systempkg");
    if (req.getParameter("diff") != null) {
      String diffVer = req.getParameter("diff").replaceAll("/|\\\\|(\\.\\.)", ""); 
      File diffDir = new File(new File(root, path[1]), diffVer);
      if (diffDir.exists()) {
        File xDelta = new File(target.getParentFile(), ".xdelta-" + diffVer);
        File original = new File(diffDir, ".systempkg");
        File parent = xDelta.getParentFile();
        if (!parent.mkdirs() || parent.exists()) {
          resp.sendError(503);
          return;
        }
        try (InputStream in = req.getInputStream();
            OutputStream out = new FileOutputStream(xDelta)) {
          new StreamPumper(in, out).run();
        }
        JarPatcher.main(new String[]{ xDelta.getAbsolutePath(), target.getAbsolutePath(), original.getAbsolutePath()});
      } else {
        resp.sendError(404);
        return;
      }
    }
    try (InputStream in = req.getInputStream();
        OutputStream out = new FileOutputStream(target)) {
      new StreamPumper(in, out).run();
    }
  }
  private static class DelegateServletConfig implements ServletConfig {
    private final ServletConfig config;
    private final ConcurrentHashMap<String, String> initParams = new ConcurrentHashMap<>();
    public DelegateServletConfig(ServletConfig config, File ... root) throws MalformedURLException {
      this.config = config;
      for (String next : new EnumerationIterable<String>(config.getInitParameterNames())) {
        initParams.put(next, config.getInitParameter(next));
      }
      initParams.put("resourceBase", root[0].getAbsolutePath());
      initParams.put("property.roots", root[0].getAbsoluteFile().toURI().toURL().toString() + "/");
      if (root.length > 1) {
        for (int i=0; i<root.length; i++) {
          String nextVal = initParams.get("property.roots") + 
              "|" + root[i].getAbsoluteFile().toURI().toURL().toString() + "/";
          initParams.put("property.roots", nextVal);
        }
      }
    }
    @Override
    public String getServletName() {
      return config.getServletName();
    }
    
    @Override
    public ServletContext getServletContext() {
      return config.getServletContext();
    }
    
    @Override
    public Enumeration<String> getInitParameterNames() {
      return new Enumeration<String>() {
        Iterator<String> it = initParams.keySet().iterator();
        @Override
        public boolean hasMoreElements() {
          return it.hasNext();
        }
        @Override
        public String nextElement() {
          return it.next();
        } 
      };
    }
    
    @Override
    public String getInitParameter(String name) {
      return initParams.get(name);
    }
  }
}
