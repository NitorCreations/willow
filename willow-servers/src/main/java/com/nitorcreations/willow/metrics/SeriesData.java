package com.nitorcreations.willow.metrics;

import java.util.ArrayList;
import java.util.List;

public class SeriesData<T,L> {
	String key;
	List<Point<T,L>> values = new ArrayList<>();
}
