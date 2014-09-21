package com.nitorcreations.willow.utils;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestPropertyMerge {
	@Test
	public void testRegex() {
		Pattern p = PropertyMerge.ARRAY_PROPERTY_REGEX;
		assertTrue(p.matcher("foo.bar[]").matches());
		assertTrue(p.matcher("foo.bar[44]").matches());
		assertFalse(p.matcher("foo.bar[4w4]").matches());
		Matcher m = p.matcher("foo.bar[44]");
		assertTrue(m.matches());
		assertEquals("foo.bar", m.group(1));
		m = p.matcher("foo.bar.plalala[]");
		assertTrue(m.matches());
		assertEquals("foo.bar.plalala", m.group(1));
	}

	@Test
	public void testMerge() {
		Properties seed = new Properties();
		seed.setProperty("target.id", "env_test");
		seed.setProperty("node-group.id", "appservers");
		seed.setProperty("node.id", "appserver");
		seed.setProperty("component.id", "webfront");
		PropertyMerge p = new PropertyMerge();
		Properties res = p.merge(seed, "root.properties");
		assertEquals("env_test", res.getProperty("target.id"));
		assertEquals("appservers", res.getProperty("node-group.id"));
		assertEquals("appserver", res.getProperty("node.id"));
		assertEquals("webfront", res.getProperty("component.id"));
		assertEquals("common/common.properties", res.getProperty("included.file[0]"));
		assertEquals("common/settings/common.properties", res.getProperty("included.file[1]"));
		assertEquals("common/settings/node-group/appservers.properties", res.getProperty("included.file[2]"));
		assertEquals("common/settings/node/appserver.properties", res.getProperty("included.file[3]"));
		assertEquals("common/settings/component/webfront.properties", res.getProperty("included.file[4]"));
		assertEquals("env_test/settings/common.properties", res.getProperty("included.file[5]"));
		assertEquals("env_test/settings/node-group/appservers.properties", res.getProperty("included.file[6]"));
		assertEquals("env_test/settings/node/appserver.properties", res.getProperty("included.file[7]"));
		assertEquals("env_test/settings/component/webfront.properties", res.getProperty("included.file[8]"));
	}
}
