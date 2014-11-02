package com.nitorcreations.willow.metrics;

import java.util.List;


public class RequestCountMetric extends SimpleMetric<Number,Long> {

	@Override
	public String getType() {
		return "access";
	}

	@Override
	public String[] requiresFields() {
		return new String[] { "duration" };
	}
	
	@Override
	protected Number estimateValue(List<Number> preceeding, long stepTime, long stepLen) {
		return preceeding.size();
	}

}
