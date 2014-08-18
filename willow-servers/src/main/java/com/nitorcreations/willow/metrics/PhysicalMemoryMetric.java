package com.nitorcreations.willow.metrics;


public class PhysicalMemoryMetric extends SimpleMetric {

	@Override
	public String getIndex() {
		return "mem";
	}

	@Override
	public String[] requiresFields() {
		return new String[] { "usedPercent" };
	}

}
