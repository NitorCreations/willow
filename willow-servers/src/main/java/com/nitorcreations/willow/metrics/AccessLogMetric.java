package com.nitorcreations.willow.metrics;

import java.util.List;
import java.util.Map;

import com.nitorcreations.willow.messages.AccessLogEntry;

public class AccessLogMetric extends FullMessageMetric<AccessLogEntry, Long, Long>{

	@Override
	protected void addValue(Map<String, SeriesData<Long, Long>> values,
			List<AccessLogEntry> preceeding, long stepTime, long stepLen) {
		long[] buckets = new long[5];
		for (AccessLogEntry next : preceeding) {
			if (next.getDuration() < 10) {
				buckets[0]++;
			} else if (next.getDuration() < 100) {
				buckets[1]++;
			} else if (next.getDuration() < 1000) {
				buckets[2]++;
			} else if (next.getDuration() < 10000) {
				buckets[3]++;
			} else {
				buckets[4]++;
			}
		}
		addBucket(values, "-9", stepTime, buckets[0]);
		addBucket(values, "10-99", stepTime, buckets[1]);
		addBucket(values, "100-999", stepTime, buckets[2]);
		addBucket(values, "1000-9999", stepTime, buckets[3]);
		addBucket(values, "10000-", stepTime, buckets[4]);
	}
	private void addBucket(Map<String, SeriesData<Long, Long>> values, String label, long time, long value) {
		SeriesData<Long, Long> bucket = values.get(label);
		if (bucket == null) {
			bucket = new SeriesData<>();
			bucket.key = label;
			values.put(label, bucket);
		}
		Point<Long, Long> p = new Point<>();
		p.x  = time;
		p.y = value;
		bucket.values.add(p);
	}
}
