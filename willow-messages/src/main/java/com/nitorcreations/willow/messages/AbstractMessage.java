package com.nitorcreations.willow.messages;

import java.util.ArrayList;
import java.util.List;

public class AbstractMessage {
	public String instance = "";
	public long timestamp = System.currentTimeMillis();
	public List<String> tags = new ArrayList<>();
	public AbstractMessage() {
		tags.add("category_" + MessageMapping.map(this.getClass()).lcName());
	}
	public void addTags(String ... tags) {
		for (String next : tags) {
			this.tags.add(next);
		}
	}
	public void addTags(List<String> tags) {
		this.tags.addAll(tags);
	}
}
