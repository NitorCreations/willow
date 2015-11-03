package com.nitorcreations.willow.servers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.DefaultServlet;

import com.google.gson.Gson;
import com.nitorcreations.willow.download.Extractor;
import com.nitorcreations.willow.properties.PropertyKeys;
import com.nitorcreations.willow.servlets.PropertyServlet;
import com.nitorcreations.willow.utils.EnumerationIterable;
import com.nitorcreations.willow.utils.FileUtil;

import at.spardat.xma.xdelta.JarPatcher;

public class DeploymentServlet extends DefaultServlet {
  public static final String DISALLOWED_NAME_VERSION_CHARACTERS = "[^a-zA-Z0-9\\._\\-]";
  public static final String SYSTEMPKG_NAME = ".systempkg.zip";
  private static final long serialVersionUID = 4507192371045140774L;
  private AtomicReference<File> root = new AtomicReference<>();
  private transient ServletConfig config;
  @Override
  public void init(final ServletConfig config) throws ServletException {
    String rootPath = config.getInitParameter("deployment.data.root");
    if (rootPath != null) {
      root.set(new File(rootPath).getAbsoluteFile());
    } else {
      root.set(new File(new File(".").getAbsoluteFile(), "deploydata"));
    }
    this.config = config;
    ServletConfig delegateConfig;
    try {
      delegateConfig = new DelegateServletConfig(config, root.get());
    } catch (MalformedURLException e) {
      throw new ServletException("Failed to initilaize deployment servlet", e);
    }
    super.init(delegateConfig);
  }
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    String[] path = req.getPathInfo().split("/");
    if (path.length < 3) {
      resp.sendError(404);
      return;
    }
    String systemName = path[1].replaceAll(DISALLOWED_NAME_VERSION_CHARACTERS, "");
    if (!new File(root.get(), systemName).exists() || !new File(root.get(), systemName).isDirectory()) {
      try (OutputStream out = resp.getOutputStream()) {
        resp.setStatus(200);
        resp.setContentType("application/json");
        out.write("[]".getBytes(StandardCharsets.UTF_8));
        out.flush();
        return;
      }
    }
    if (path[2].equals("diffcandidates")) {
      ArrayList<String> ret = new ArrayList<>();
      File[] versions = new File(root.get(), systemName).listFiles();
      if (versions != null) {
        Arrays.sort(versions, new Comparator<File>() {
          @Override
          public int compare(File o1, File o2) {
            return (int)(o2.lastModified() - o1.lastModified());
          }
        });
        for (File next : versions) {
          ret.add(next.getName());
        }
      }
      try (OutputStream out = resp.getOutputStream()) {
        resp.setStatus(200);
        String output = new Gson().toJson(ret);
        resp.setContentType("application/json");
        out.write(output.getBytes(StandardCharsets.UTF_8));
        out.flush();
        return;
      }
    }
    String systemVer =  path[2].replaceAll(DISALLOWED_NAME_VERSION_CHARACTERS, "");
    if (!new File(new File(root.get(), systemName), systemVer).exists()) {
      resp.sendError(404);
      return;
    }
    if (path.length == 3 || path[3].isEmpty()) {
      File pkg = new File(new File(new File(root.get(), systemName), systemVer), SYSTEMPKG_NAME);
      if (!pkg.exists()) {
        resp.sendError(404);
        return;
      }
      resp.setStatus(200);
      resp.setContentType("application/zip");
      try (FileInputStream in = new FileInputStream(pkg);
          OutputStream out = resp.getOutputStream()) {
        FileUtil.copy(in, out);
      }
      return;
    }
    if (path[3].equals("properties")) {
      File pkg = new File(new File(new File(root.get(), systemName), systemVer), "properties");
      PropertyServlet propertyServlet = new PropertyServlet();
      propertyServlet.init(new DelegateServletConfig(config, root.get(), pkg));
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
    if (path.length < 3 || path.length > 4) {
      resp.sendError(404);
      return;
    }
    String systemName = path[1].replaceAll(DISALLOWED_NAME_VERSION_CHARACTERS, "");
    File systemRoot = new File(root.get(), systemName);
    if (!(systemRoot.mkdirs() || systemRoot.exists())) {
      resp.sendError(404);
      return;
    }
    if (path.length > 3 && !path[4].isEmpty()) {
      resp.sendError(404);
      return;
    }
    String systemVer =  path[2].replaceAll(DISALLOWED_NAME_VERSION_CHARACTERS, "");
    File versionRoot = new File(systemRoot, systemVer);
    if (!(versionRoot.mkdirs() || versionRoot.exists())) {
      resp.sendError(404);
      return;
    }
    File target = new File(versionRoot, SYSTEMPKG_NAME);
    if (req.getParameter("diff") != null) {
      String diffVer = req.getParameter("diff").replaceAll(DISALLOWED_NAME_VERSION_CHARACTERS, "");
      File diffDir = new File(systemRoot, diffVer);
      if (diffDir.exists() && diffDir.isDirectory()) {
        File xDelta = new File(versionRoot, ".xdelta-" + diffVer);
        File original = new File(diffDir, SYSTEMPKG_NAME);
        if (!original.exists()) {
          resp.sendError(404, "Diff original not found");
          return;
        }
        try (InputStream in = req.getInputStream();
            OutputStream out = new FileOutputStream(xDelta)) {
          FileUtil.copy(in, out);
        }
        JarPatcher.main(new String[]{ xDelta.getAbsolutePath(), target.getAbsolutePath(), original.getAbsolutePath()});
      } else {
        resp.sendError(404, "Diff version not found" );
        return;
      }
    } else {
      try (InputStream in = req.getInputStream();
          OutputStream out = new FileOutputStream(target)) {
        FileUtil.copy(in, out);
      }
    }
    Properties extAll = new Properties();
    extAll.setProperty(PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_GLOB, "**");
    extAll.setProperty(PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_ROOT, versionRoot.getAbsolutePath());
    Extractor extract = new Extractor(extAll, target);
    extract.call();
    File props = new File(versionRoot, "properties.jar");
    File propertiesRoot = new File(versionRoot, "properties");
    if (!props.exists()) {
      resp.sendError(503, "properties.jar not found");
      return;
    }
    if (!propertiesRoot.mkdirs() || !(propertiesRoot.exists() && propertiesRoot.isDirectory())) {
      resp.sendError(503, "Failed to create properties extract root");
      return;
      
    }
    extAll.setProperty(PropertyKeys.PROPERTY_KEY_SUFFIX_EXTRACT_ROOT, propertiesRoot.getAbsolutePath());
    Extractor extractProps = new Extractor(extAll, props);
    extractProps.call();
    resp.setStatus(200);
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
