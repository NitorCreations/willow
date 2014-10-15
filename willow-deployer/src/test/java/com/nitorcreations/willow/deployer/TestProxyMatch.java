package com.nitorcreations.willow.deployer;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestProxyMatch {
	@Test
	public void testProxyMatch() {
		assertFalse(DeployerControl.noProxyMatches("foo.bar.com", "localhost,127.0.0.1,localaddress,.localdomain.com"));
		assertTrue(DeployerControl.noProxyMatches("localhost", "localhost,127.0.0.1,localaddress,.localdomain.com"));
		assertTrue(DeployerControl.noProxyMatches("localaddress", "localhost,127.0.0.1,localaddress,.localdomain.com"));
		assertTrue(DeployerControl.noProxyMatches("localaddress.localdomain.com", "localhost,127.0.0.1,localaddress,.localdomain.com"));
		assertTrue(DeployerControl.noProxyMatches(".localdomain.com", "localhost,127.0.0.1,localaddress,.localdomain.com"));
	}
}
