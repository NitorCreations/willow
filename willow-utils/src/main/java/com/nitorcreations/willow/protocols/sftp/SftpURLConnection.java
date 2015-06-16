package com.nitorcreations.willow.protocols.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.logging.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.nitorcreations.willow.utils.Obfuscator;

public class SftpURLConnection extends URLConnection {
  private final Logger log = Logger.getLogger(getClass().getCanonicalName());
  private Proxy proxy;
  public SftpURLConnection(URL url) {
    super(url);
  }
  public SftpURLConnection(URL url, Proxy proxy) {
    super(url);
    this.proxy =proxy;
  }
  public static final int DEFAULT_TIMEOUT = 30000;
  private Session session;
  private ChannelSftp channel;
  private boolean connected = false;

  @Override
  public void connect() throws IOException {
    if (connected) {
      return;
    }
    String[] userInfo = null;
    connected = true;
    if (url.getUserInfo() == null) {
      userInfo = new String[] { System.getProperty("user.name") };
    } else {
      userInfo = url.getUserInfo().split(":");
      if (userInfo[0].isEmpty()) {
        userInfo[0] = System.getProperty("user.name");
      }
    }
    JSch jsch = new JSch();
    Connector connector = null;
    try {
      connector = ConnectorFactory.getDefault().createConnector();
    } catch (AgentProxyException e) {
      log.fine("Failed to get ssh agent connector:" + e.getMessage());
    }
    if (connector != null) {
      jsch.setIdentityRepository(new RemoteIdentityRepository(connector));
    }
    try {
      String privateKey = System.getProperty("ssh.private.key");
      if (privateKey != null) {
        jsch.addIdentity(privateKey);
      }
      int port = url.getPort() > 0 ? url.getPort() : url.getDefaultPort();
      session = jsch.getSession(userInfo[0], url.getHost(), port);
      if (userInfo.length > 1) {
        if (userInfo[1].startsWith("obf:")) {
          userInfo[1] = new Obfuscator().decrypt(userInfo[1].substring(4));
        }
        session.setPassword(userInfo[1]);
      }
      Properties config = new Properties();
      config.put("StrictHostKeyChecking", "no");
      session.setConfig(config);
      session.setTimeout(DEFAULT_TIMEOUT);
      if (proxy != null) {
        String proxyHost = ((InetSocketAddress)proxy.address()).getHostName() + ":" + ((InetSocketAddress)proxy.address()).getPort();
        if (proxy.type() == Proxy.Type.HTTP) {
          session.setProxy(new com.jcraft.jsch.ProxyHTTP(proxyHost));
        } else if (proxy.type() == Proxy.Type.SOCKS) {
          session.setProxy(new com.jcraft.jsch.ProxySOCKS5(proxyHost));
        }
      }
      session.connect();
      channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect();
    } catch (JSchException e) {
      disconnect();
      throw new IOException("Can't connect using " + url, e);
    }
  }

  public void disconnect() throws IOException {
    if (channel != null) {
      channel.disconnect();
    }
    if (session != null) {
      session.disconnect();
    }
  }

  public ChannelSftp getChannel() throws IOException {
    if (channel == null) {
      connect();
    }
    return channel;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    String filePath = getPath();
    if (filePath.startsWith("/~/")) {
      filePath = filePath.substring(3);
    }
    try {
      return getChannel().get(filePath);
    } catch (SftpException e) {
      throw new IOException("Can't retrieve " + filePath + " from " + url.getHost() + " because " + e.getMessage());
    }
  }

  protected String getPath() {
    return url.getPath();
  }
}
