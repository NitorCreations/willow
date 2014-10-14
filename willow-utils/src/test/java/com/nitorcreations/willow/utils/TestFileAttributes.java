package com.nitorcreations.willow.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;

import org.junit.Test;

public class TestFileAttributes {
	@Test
	public void testAcl() throws IOException {
		/*
		File pom = new File("pom.xml");
		AclFileAttributeView acl = Files.getFileAttributeView(pom.toPath(), AclFileAttributeView.class);
		for (AclEntry next : acl.getAcl()) {
			System.out.println(next);
		}*/
	}

}
