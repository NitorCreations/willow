package com.nitorcreations.willow.metrics;

import java.util.List;

import org.elasticsearch.action.search.SearchResponse;


public class CpuBusyMetric extends SimpleMetric<CpuData> {
	CpuData prevValues;
	double prevRes=0D;
	@Override
	protected CpuData getValue(List<Number> results) {
		CpuData ret = new CpuData(this, results.get(0).longValue(), results.get(1).longValue());
		if (prevValues == null) prevValues = ret;
		return ret;
	}
	@Override
	protected void readResponse(SearchResponse response) {
		prevValues = null;
		prevRes = 0D;
		super.readResponse(response);
	}
	@Override
	public String getType() {
		return "cpu";
	}
	@Override
	public String[] requiresFields() {
		return new String[] {"idle", "total" };
	}
	@Override
	protected Double estimateValue(List<CpuData> preceeding, Long stepTime) {
		CpuData last = preceeding.get(preceeding.size() - 1);
		long idleEnd = last.idle;
		long totalEnd = last.total;
		long idleStart = preceeding.get(0).idle;
		long totalStart = preceeding.get(0).total;
		if (preceeding.size() == 1) {
			idleStart = prevValues.idle;
			totalStart = prevValues.total;
		}
		prevValues = last;
		long totalDiff = totalEnd - totalStart;
		if (totalDiff == 0) return fillMissingValue();
		long idleDiff = idleEnd - idleStart;
		return prevRes = (100 * (totalDiff - idleDiff))/(double)totalDiff; 
	}
	@Override
	protected Double fillMissingValue() {
		return 0D;
	}
}
