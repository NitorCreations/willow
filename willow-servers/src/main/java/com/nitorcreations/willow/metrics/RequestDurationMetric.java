package com.nitorcreations.willow.metrics;

import java.util.List;


public class RequestDurationMetric extends SimpleMetric<Number> {

	@Override
	public String getType() {
		return "access";
	}

	@Override
	public String[] requiresFields() {
		return new String[] { "duration" };
	}
	
	@Override
	protected Number estimateValue(List<Number> preceeding, Long stepTime) {
		long sum = 0;
		for (Object next : preceeding) {
			sum += ((Number)next).longValue();
		}
		return sum / preceeding.size();
	}

}
