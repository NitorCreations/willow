package com.nitorcreations.willow.eventhandler;

import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.nitorcreations.willow.messages.event.EventMessage;

/**
 * Event handler to send email with SMTP.
 * 
 * @author mtommila
 */
public class SendMailEventHandler implements EventHandler {

  private Logger logger = Logger.getLogger(getClass().getName());

  private String host;
  private int port = 25;
  private String username;
  private String password;
  private boolean ssl;
  private boolean tls;
  private String from;
  private String to;

  @Override
  public void handle(EventMessage eventMessage) throws Exception {
    logger.fine("Sending mail to " + host + ":" + port);

    Properties properties = new Properties();
    properties.put("mail.smtp.host", host);
    properties.put("mail.smtp.port", String.valueOf(port));
 
    Session session;
    if (ssl) {
      properties.put("mail.smtp.socketFactory.port", String.valueOf(port));
      properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    } else if (tls) {
      properties.put("mail.smtp.starttls.enable", "true");
    }
    if (username != null && password != null) {
      properties.put("mail.smtp.auth", "true");
      session = Session.getInstance(properties, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(username, password);
        }
      });
    } else {
      session = Session.getInstance(properties);
    }
 
    Message message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
    message.setSubject(eventMessage.eventType);
    message.setText(eventMessage.description);

    Transport.send(message);

    logger.fine("Mail sent");
  }

  /**
   * Set the SMTP hostname where the mail is sent.
   * 
   * @param host The hostname of the SMTP server.
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Set the port of the SMTP server.
   * 
   * @param port The port of the SMTP server.
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Set the username used for authenticating to the SMTP server.
   * 
   * @param username The username used for authenticating to the SMTP server.
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Set the password used for authenticating to the SMTP server.
   * 
   * @param password The password used for authenticating to the SMTP server.
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Set if SSL is used for the SMTP server connection.
   * 
   * @param ssl If SSL is used.
   */
  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  /**
   * Set if TLS is used for the SMTP server connection.
   * 
   * @param tls If TLS is used.
   */
  public void setTls(boolean tls) {
    this.tls = tls;
  }

  /**
   * Set the "from" field of the emails sent.
   * 
   * @param from The "from" address in RFC822 format.
   */
  public void setFrom(String from) {
    this.from = from;
  }

  /**
   * Set the "to" field of the emails sent.
   * 
   * @param to Comma-separated list of email addresses in RFC822 format.
   */
  public void setTo(String to) {
    this.to = to;
  }
}
