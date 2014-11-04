package com.nitorcreations.willow.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class DiskIOMetric extends SimpleMetric<DiskIOData,Object> {
	private HashMap<String, DiskIOData> prevValues = new HashMap<>();

	@Override
	public String getType() {
		return "diskio";
	}

	@Override
	public String[] requiresFields() {
		return new String[] { "device", "readBytes", "writeBytes" };
	}
	@Override
	protected DiskIOData getValue(List<Object> results) {
		DiskIOData ret = new DiskIOData((String)results.get(0), ((Number)results.get(1)).longValue(), ((Number)results.get(2)).longValue());
		if (!prevValues.containsKey(ret.device)) prevValues.put(ret.device, ret);
		return ret;
	}
	@Override
	protected Double estimateValue(List<DiskIOData> preceeding, long stepTime, long stepLen) {
		HashMap<String, DiskIOData> lasts = new HashMap<>();
			
		for (DiskIOData next : preceeding) {
			lasts.put(next.device, next);
		}
		
		long netBytes = 0;
		for (Entry<String, DiskIOData> next : lasts.entrySet()) {
			DiskIOData start = prevValues.get(next.getKey());
			if (start == null) continue;
			netBytes += (next.getValue().read - start.read);
			netBytes += (next.getValue().write - start.write);
		}
		prevValues.putAll(lasts);
		return (1000 * netBytes) / (double)(1024 * stepLen) ;
	}
	@Override
	protected Double fillMissingValue() {
		return 0D;
	}

}
