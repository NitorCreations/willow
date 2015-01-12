package com.nitorcreations.willow.deployer;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

import com.nitorcreations.willow.utils.MergeableProperties;

public class TestExtract {
	@Test
	public void testExtract() throws IOException {
		MergeableProperties props = new MergeableProperties();
		props.setProperty("deployer.download.directory", "target/test-download");
		props.setProperty("deployer.download.url[]", "file:./target/test-classes/test.zip");
		props.setProperty("deployer.download.url[last].extract.glob", "**");
		props.setProperty("deployer.download.url[last].extract.root", "target/test-extract");
		props.setProperty("deployer.download.url[]", "file:./target/test-classes/test.zip");
		props.setProperty("deployer.download.url[last].extract.glob", "**");
		props.setProperty("deployer.download.url[last].extract.root", "target/test2-extract");
		props.setProperty("coremedia.admin.user", "foo");
		props.setProperty("coremedia.admin.password", "bar");
		props.setProperty("debug.comment", "#");
		props.setProperty("deployer.download.url[last].extract.filter.glob", "**.sh|**.xml|**.properties|**.jpif");
		PreLaunchDownloadAndExtract dwnld = new PreLaunchDownloadAndExtract(props);
		dwnld.call();
		File res = new File("target/test-extract/bin/graceful.sh");
		assertTrue(res.exists());
		assertTrue(res.canExecute());
		res = new File("target/test2-extract/bin/cm");
		assertTrue(res.exists());
		assertTrue(res.canExecute());
		for (String line : Files.readAllLines(res.toPath())) {
			assertFalse(line.contains("@coremedia.admin.user@"));
		}
	}
}