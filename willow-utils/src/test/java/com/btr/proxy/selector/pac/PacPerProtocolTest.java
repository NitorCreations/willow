package com.btr.proxy.selector.pac;

import static com.btr.proxy.selector.pac.TestUtil.toUrl;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Test;

public class PacPerProtocolTest {

  /*************************************************************************
   * Test the PAC selector for a given protocol.
   * 
   * @throws IOException
   *           of read error.
   * @throws URISyntaxException
   *           on uri syntax error.
   ************************************************************************/
  @Test
  public void testPacForSocket() throws IOException, URISyntaxException {
    List<Proxy> result = new PacProxySelector(new UrlPacScriptSource(
        toUrl("test1.pac"))).select(TestUtil.SOCKET_TEST_URI);
    assertEquals(TestUtil.HTTP_TEST_PROXY, result.get(0));
  }
}
