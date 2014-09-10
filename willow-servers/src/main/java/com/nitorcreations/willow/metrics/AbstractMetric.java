package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public abstract class AbstractMetric implements Metric {
	protected String[] getIndexes(long start, long end) {
		ArrayList<String> ret = new ArrayList<>();
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(new Date(start));
		startCal.set(Calendar.SECOND, 1);
		startCal.set(Calendar.MINUTE, 0);
		startCal.set(Calendar.HOUR_OF_DAY, 0);

		Calendar endCal = Calendar.getInstance();
		endCal.setTime(new Date(end));
		
		while (startCal.before(endCal)) {
			ret.add(String.format("%04d", startCal.get(Calendar.YEAR)) + "-" + 
				String.format("%02d", (startCal.get(Calendar.MONTH) + 1)) + 
				"-" + String.format("%02d", startCal.get(Calendar.DAY_OF_MONTH)));
			startCal.add(Calendar.DAY_OF_YEAR, 1);
		}
		return ret.toArray(new String[ret.size()]);
	}

}
