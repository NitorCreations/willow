package com.nitorcreations.willow.deployer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

import com.nitorcreations.willow.deployer.PreLaunchDownloadAndExtract;
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
    props.setProperty("deployer.download.url[last].extract.filter.glob", "**.sh|**.xml|**.properties|**.jpif");
    props.setProperty("deployer.download.url[]", "property:coremedia.admin.user");
    props.setProperty("deployer.download.url[last].ignore.md5", "true");
    props.setProperty("coremedia.admin.user", "foo");
    props.setProperty("coremedia.admin.password", "bar");
    props.setProperty("debug.comment", "#");
    PreLaunchDownloadAndExtract dwnld = new PreLaunchDownloadAndExtract(props);
    dwnld.call();
    File res = new File("target/test-extract/bin/graceful.sh");
    File prop = new File("target/test-download/coremedia.admin.user");
    assertTrue(res.exists());
    assertTrue(res.canExecute());
    assertTrue(prop.exists());
    assertEquals("foo", Files.readAllLines(prop.toPath(), StandardCharsets.UTF_8).get(0));
    res = new File("target/test2-extract/bin/cm");
    assertTrue(res.exists());
    assertTrue(res.canExecute());
    for (String line : Files.readAllLines(res.toPath(), StandardCharsets.UTF_8)) {
      assertFalse(line.contains("@coremedia.admin.user@"));
    }
  }
}
