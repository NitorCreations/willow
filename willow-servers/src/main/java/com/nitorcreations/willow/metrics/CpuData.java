package com.nitorcreations.willow.metrics;

class CpuData {
	/**
	 * 
	 */
	private final CpuBusyMetric CpuData;
	public CpuData(CpuBusyMetric cpuBusyMetric, long idle, long total) {
		CpuData = cpuBusyMetric;
		this.idle = idle;
		this.total = total;
	}
	long idle;
	long total;
}