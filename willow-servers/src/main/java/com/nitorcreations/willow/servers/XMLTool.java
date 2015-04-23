package com.nitorcreations.willow.servers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.nitorcreations.willow.utils.ProxyUtils;

public class XMLTool {
  private static final XPathFactory factory = XPathFactory.newInstance(); ;
  private static final class DummyEntityResolver implements EntityResolver {
    public InputSource resolveEntity(String publicID, String systemID) throws SAXException {
      return new InputSource(new StringReader(""));
    }
  }
  private static Logger logger = Logger.getLogger(XMLTool.class.getCanonicalName());

  public static Node parse(String url) {
    try (InputStream in = ProxyUtils.getUriInputStream(null, null, url)) {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = null;
      dbf.setValidating(false);
      dbf.setCoalescing(true);
      dbf.setNamespaceAware(true);

      db = dbf.newDocumentBuilder();
      db.setEntityResolver(new DummyEntityResolver());
      Document doc = db.parse(new InputSource(in));
      return doc.getDocumentElement();
    } catch (ParserConfigurationException e) {
      logger.log(Level.FINE, "XML parsing configuration error", e);
    } catch (SAXException e) {
      logger.log(Level.FINE, "XML parsing failed", e);
    } catch (IOException e) {
      logger.log(Level.FINE, "Unable to read XML file", e);
    } catch (URISyntaxException e) {
      logger.log(Level.FINE, "Invalid url '" + url + "'", e);
    }
    return null;
  }
  public static Iterator<Node> xpath(String expression, Node context) {
    XPath xpath = factory.newXPath();
    try {
      NodeList ret = (NodeList) xpath.evaluate(expression, context, XPathConstants.NODESET);
      return new IterableNodeList(ret).iterator();
    } catch (XPathExpressionException e) {
      logger.log(Level.FINE, "Invalid XPath expression", e);
    }
    return null;
  }
}
