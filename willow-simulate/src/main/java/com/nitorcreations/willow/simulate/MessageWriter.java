package com.nitorcreations.willow.simulate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import com.nitorcreations.willow.messages.AbstractMessage;
import com.nitorcreations.willow.messages.MessageMapping;

public class MessageWriter {
	private String fileName;
	private List<MessageProducer> producers;
	private final MessageMapping msgMap = new MessageMapping();
	public MessageWriter(String fileName, List<MessageProducer> producers) {
		this.fileName = fileName;
		this.producers = producers;
	}
	
	public void write(boolean append) throws IOException {
		TreeMap<Long, AbstractMessage> messages = new TreeMap<>();
		for (MessageProducer next : producers) {
			AbstractMessage nextMessage = next.next();
			while (nextMessage != null) {
				messages.put(Long.valueOf(nextMessage.timestamp), nextMessage);
			}
		}
		ArrayList<AbstractMessage> orderedMessages = new ArrayList<>();
		orderedMessages.addAll(messages.values());
		ByteBuffer buf = msgMap.encode(orderedMessages);
		try (FileOutputStream out = new FileOutputStream(new File(fileName), append);
				GZIPOutputStream gzout = new GZIPOutputStream(out)) {
			gzout.write(buf.array(), buf.position(), buf.limit() - buf.position());
		}
	}
}
