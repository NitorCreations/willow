package com.nitorcreations.willow.deployer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingStreamPumper extends AbstractStreamPumper implements Runnable {
	private BufferedReader in;
	private Logger log;
	private Level level;

	public LoggingStreamPumper(InputStream in, Level level, String name) throws URISyntaxException {
		super(in, name);
		this.level = level;
		this.log = Logger.getLogger(name);
	}

	@Override
	public void handle(String line) {
		log.log(level, line);
	}
}
