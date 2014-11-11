package com.nitorcreations.willow.utils;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestPropertyMerge {
	@Test
	public void testRegex() {
		Pattern p = MergeableProperties.ARRAY_PROPERTY_REGEX;
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
	public void testRegex1() {
		Pattern p = MergeableProperties.ARRAY_REFERENCE_REGEX;
		Matcher m = p.matcher("foo.bar[last].extractroot");
		assertTrue(m.matches());
		assertNull(m.group(1));
		assertEquals("foo.bar", m.group(2));
		assertEquals(".extractroot", m.group(3));
		m = p.matcher("${foo.bar[last].extractroot[]}");
		assertTrue(m.matches());
		assertEquals("${", m.group(1));
		assertEquals("foo.bar", m.group(2));
		assertEquals(".extractroot[]}", m.group(3));
		m = MergeableProperties.ARRAY_PROPERTY_REGEX.matcher("${foo.bar[1].extractroot[]}");
		assertTrue(m.matches());
		assertEquals("${foo.bar[1].extractroot", m.group(1));
		assertEquals("}", m.group(2));
		m = p.matcher("foo.bar.plalala[].doo");
		assertFalse(m.matches());
	}
	@Test
	public void testObfuscator() {
		Obfuscator o = new Obfuscator("foobar");
		String obfuscated = o.encrypt("baz");
		assertEquals("baz", o.decrypt(obfuscated));
	}
	@Test
	public void testMerge() {
		Properties seed = new Properties();
		seed.setProperty("target.id", "env_test");
		seed.setProperty("node-group.id", "appservers");
		seed.setProperty("node.id", "appserver");
		seed.setProperty("component.id", "webfront");
		MergeableProperties p = new MergeableProperties();
		Properties res = p.merge(seed, "root.properties");
		assertEquals("env_test", res.getProperty("target.id"));
		assertEquals("appservers", res.getProperty("node-group.id"));
		assertEquals("appserver", res.getProperty("node.id"));
		assertEquals("webfront", res.getProperty("component.id"));
		assertEquals("common/common.properties", res.getProperty("included.file[0]"));
		assertEquals("common/settings/common.properties", res.getProperty("included.file[1]"));
		assertEquals("foo/bar", res.getProperty("included.file[1].extraprops"));
		assertEquals("foo/bar", res.getProperty("included.file[1].foo[0]"));
		assertEquals("foo/bar", res.getProperty("included.file[1].foo[0].bar[0]"));
		assertEquals("foo/bar", res.getProperty("foo.bar"));
		assertEquals("common/settings/node-group/appservers.properties", res.getProperty("included.file[2]"));
		assertEquals("common/settings/node/appserver.properties", res.getProperty("included.file[3]"));
		assertEquals("common/settings/component/webfront.properties", res.getProperty("included.file[4]"));
		assertEquals("env_test/settings/common.properties", res.getProperty("included.file[5]"));
		assertEquals("env_test/settings/node-group/appservers.properties", res.getProperty("included.file[6]"));
		assertEquals("env_test/settings/node/appserver.properties", res.getProperty("included.file[7]"));
		assertEquals("env_test/settings/component/webfront.properties", res.getProperty("included.file[8]"));
	}
	@Test
	public void testMerge1() {
		Properties seed = new Properties();
		seed.setProperty("target.id", "env_test");
		seed.setProperty("node-group.id", "appservers");
		seed.setProperty("node.id", "appserver");
		seed.setProperty("component.id", "webfront");
		MergeableProperties p = new MergeableProperties("file:./target/test-classes/");
		Properties res = p.merge(seed, "root.properties");
		assertEquals("env_test", res.getProperty("target.id"));
		assertEquals("appservers", res.getProperty("node-group.id"));
		assertEquals("appserver", res.getProperty("node.id"));
		assertEquals("webfront", res.getProperty("component.id"));
		assertEquals("common/common.properties", res.getProperty("included.file[0]"));
		assertEquals("common/settings/common.properties", res.getProperty("included.file[1]"));
		assertEquals("foo/bar", res.getProperty("included.file[1].extraprops"));
		assertEquals("common/settings/node-group/appservers.properties", res.getProperty("included.file[2]"));
		assertEquals("common/settings/node/appserver.properties", res.getProperty("included.file[3]"));
		assertEquals("common/settings/component/webfront.properties", res.getProperty("included.file[4]"));
		assertEquals("env_test/settings/common.properties", res.getProperty("included.file[5]"));
		assertEquals("env_test/settings/node-group/appservers.properties", res.getProperty("included.file[6]"));
		assertEquals("env_test/settings/node/appserver.properties", res.getProperty("included.file[7]"));
		assertEquals("env_test/settings/component/webfront.properties", res.getProperty("included.file[8]"));
		assertEquals("431", res.getProperty("test.scripting"));
	}
	@Test
	public void testMerge2() {
		Properties seed = new Properties();
		seed.setProperty("target.id", "env_test");
		seed.setProperty("node-group.id", "appservers");
		seed.setProperty("node.id", "appserver");
		seed.setProperty("component.id", "webfront");
		MergeableProperties p = new MergeableProperties("classpath:", "file:./target/test-classes/");
		p.merge(seed, "root.properties");
		p.deObfuscate(new PropertySource() {
			Obfuscator o = new Obfuscator("foobar");
			@Override
			public String getProperty(String key) {
				return o.decrypt(key);
			}
		}, "obf:");
		assertEquals("env_test", p.getProperty("target.id"));
		assertEquals("appservers", p.getProperty("node-group.id"));
		assertEquals("appserver", p.getProperty("node.id"));
		assertEquals("webfront", p.getProperty("component.id"));
		assertEquals("common/common.properties", p.getProperty("included.file[0]"));
		assertEquals("common/common.properties", p.getProperty("included.file[1]"));
		assertEquals("common/settings/common.properties", p.getProperty("included.file[2]"));
		assertEquals("common/settings/common.properties", p.getProperty("included.file[3]"));
		assertEquals("common/settings/node-group/appservers.properties", p.getProperty("included.file[4]"));
		assertEquals("common/settings/node-group/appservers.properties", p.getProperty("included.file[5]"));
		assertEquals("common/settings/node/appserver.properties", p.getProperty("included.file[6]"));
		assertEquals("common/settings/node/appserver.properties", p.getProperty("included.file[7]"));
		assertEquals("common/settings/component/webfront.properties", p.getProperty("included.file[8]"));
		assertEquals("common/settings/component/webfront.properties", p.getProperty("included.file[9]"));
		assertEquals("env_test/settings/common.properties", p.getProperty("included.file[10]"));
		assertEquals("env_test/settings/common.properties", p.getProperty("included.file[11]"));
		assertEquals("env_test/settings/node-group/appservers.properties", p.getProperty("included.file[12]"));
		assertEquals("env_test/settings/node-group/appservers.properties", p.getProperty("included.file[13]"));
		assertEquals("env_test/settings/node/appserver.properties", p.getProperty("included.file[14]"));
		assertEquals("env_test/settings/node/appserver.properties", p.getProperty("included.file[15]"));
		assertEquals("env_test/settings/component/webfront.properties", p.getProperty("included.file[16]"));
		assertEquals("env_test/settings/component/webfront.properties", p.getProperty("included.file[17]"));
		assertEquals("baz", p.getProperty("obfuscated.value"));
		assertEquals("obf:foobar", p.getProperty("fake.obsfuscated.value"));
	}
}
