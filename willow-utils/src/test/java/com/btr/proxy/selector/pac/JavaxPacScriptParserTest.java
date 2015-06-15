package com.btr.proxy.selector.pac;

import static com.btr.proxy.selector.pac.TestUtil.toUrl;

import java.net.MalformedURLException;
import java.util.Calendar;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/*****************************************************************************
 * Tests for the javax.script PAC script parser.
 * 
 * @author Bernd Rosstauscher (proxyvole@rosstauscher.de) Copyright 2009
 ****************************************************************************/

public class JavaxPacScriptParserTest {

  /*************************************************************************
   * Set calendar for date and time base tests. Current date for all tests is:
   * 15. December 1994 12:00.00 its a Thursday
   ************************************************************************/
  @BeforeClass
  public static void setup() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 1994);
    cal.set(Calendar.MONTH, Calendar.DECEMBER);
    cal.set(Calendar.DAY_OF_MONTH, 15);
    cal.set(Calendar.HOUR_OF_DAY, 12);
    cal.set(Calendar.MINUTE, 00);
    cal.set(Calendar.SECOND, 00);
    cal.set(Calendar.MILLISECOND, 00);

    // TODO Rossi 26.08.2010 need to fake time
    // JavaxPacScriptParser.setCurrentTime(cal);
  }

  /*************************************************************************
   * Cleanup after the tests.
   ************************************************************************/
  @AfterClass
  public static void teadDown() {
    // JavaxPacScriptParser.setCurrentTime(null);
  }

  /*************************************************************************
   * Test method
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  public void testScriptExecution() throws Exception {
    PacScriptParser p = new JavaxPacScriptParser(new UrlPacScriptSource(
        toUrl("test1.pac")));
    p.evaluate(TestUtil.HTTP_TEST_URI.toString(), "host1.unit-test.invalid");
  }

  /*************************************************************************
   * Test method
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  public void testCommentsInScript() throws Exception {
    PacScriptParser p = new JavaxPacScriptParser(new UrlPacScriptSource(
        toUrl("test2.pac")));
    p.evaluate(TestUtil.HTTP_TEST_URI.toString(), "host1.unit-test.invalid");
  }

  /*************************************************************************
   * Test method
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  @Ignore
  // Test deactivated because it will not run in Java 1.5 and time based test
  // are unstable
  public void testScriptWeekDayScript() throws Exception {
    PacScriptParser p = new JavaxPacScriptParser(new UrlPacScriptSource(
        toUrl("testWeekDay.pac")));
    p.evaluate(TestUtil.HTTP_TEST_URI.toString(), "host1.unit-test.invalid");
  }

  /*************************************************************************
   * Test method
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  @Ignore
  // Test deactivated because it will not run in Java 1.5 and time based test
  // are unstable
  public void testDateRangeScript() throws Exception {
    PacScriptParser p = new JavaxPacScriptParser(new UrlPacScriptSource(
        toUrl("testDateRange.pac")));
    p.evaluate(TestUtil.HTTP_TEST_URI.toString(), "host1.unit-test.invalid");
  }

  /*************************************************************************
   * Test method
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  @Ignore
  // Test deactivated because it will not run in Java 1.5 and time based test
  // are unstable
  public void testTimeRangeScript() throws Exception {
    PacScriptParser p = new JavaxPacScriptParser(new UrlPacScriptSource(
        toUrl("testTimeRange.pac")));
    p.evaluate(TestUtil.HTTP_TEST_URI.toString(), "host1.unit-test.invalid");
  }

  /*************************************************************************
   * Test method
   * 
   * @throws ProxyException
   *           on proxy detection error.
   * @throws MalformedURLException
   *           on URL erros
   ************************************************************************/
  @Test
  public void methodsShouldReturnJsStrings() throws Exception {
    PacScriptParser p = new JavaxPacScriptParser(new UrlPacScriptSource(
        toUrl("testReturnTypes.pac")));
    String actual = p.evaluate(TestUtil.HTTP_TEST_URI.toString(),
        "host1.unit-test.invalid");
    Assert.assertEquals("number boolean string", actual);
  }

}
