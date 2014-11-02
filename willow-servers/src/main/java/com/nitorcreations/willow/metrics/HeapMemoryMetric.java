package com.nitorcreations.willow.metrics;


public class HeapMemoryMetric extends SimpleMetric<Long,Long> {

	@Override
	public String getType() {
		return "jmx";
	}

	@Override
	public String[] requiresFields() {
		return new String[] { "heapMemory" };
	}
}