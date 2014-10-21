package com.nitorcreations.willow.utils;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingStreamPumper extends AbstractStreamPumper implements Runnable {
	private Logger log;
	private Level level;

	public LoggingStreamPumper(InputStream in, Level level, String name) {
		super(in, name);
		this.level = level;
		this.log = Logger.getLogger(name);
	}

	@Override
	public void handle(String line) {
		log.log(level, line);
	}
}