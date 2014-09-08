package com.nitorcreations.willow.metrics;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.gson.Gson;

public class MetricsServlet implements Servlet {
	private static Node node;

	Map<String, Metric> metrics = new HashMap<String, Metric>();
	ServletConfig config;
	private SecureRandom random = new SecureRandom();
	private Settings settings;

	@Override
	public void init(ServletConfig config) throws ServletException {
		this.config = config;
		metrics.put("/heap", new HeapMemoryMetric());
		metrics.put("/mem", new PhysicalMemoryMetric());
		metrics.put("/requests", new RequestCountMetric());
		metrics.put("/latency", new RequestDurationMetric());
		setupElasticSearch(config.getServletContext());
	}

	@Override
	public ServletConfig getServletConfig() {
		return config;
	}

	@Override
	public void service(ServletRequest req, ServletResponse res)
			throws ServletException, IOException {
		if (!((HttpServletRequest)req).getMethod().equals("GET")) {
			((HttpServletResponse)res).sendError(405, "Only GET allowed");
			return;
		}
		Client client = getClient();
		String metricKey = ((HttpServletRequest)req).getPathInfo();
		Metric metric = metrics.get(metricKey);
		if (metric == null) {
			((HttpServletResponse)res).sendError(404, "Metric " + metricKey + " not found");
			return;
		}
		long start = Long.parseLong(req.getParameter("start"));
		long stop = Long.parseLong(req.getParameter("stop"));
		int step = Integer.parseInt(req.getParameter("step"));
		String tag = req.getParameter("tag");
		double[] buckets = null;
		if  (metric instanceof HistogramMetric) {
			String[] bucketsStr = req.getParameter("buckets").split(",");
			buckets = new double[bucketsStr.length];
			int i=0;
			for (String next : bucketsStr) {
				buckets[i++] = Double.parseDouble(next);
			}
		}
		List<SearchResponse> responses = new ArrayList<>();
		for (String index : getIndexes(start, stop)) {
			responses.add(client.prepareSearch(index, metric.getType())
					.setQuery(QueryBuilders.rangeQuery("timestamp")
							.from(start - step)
							.to(stop + step)
							.includeLower(false)
							.includeUpper(true))
							.setSearchType(SearchType.QUERY_AND_FETCH)
							.setSize(50000)
							.addField("timestamp")
							.addFields(metric.requiresFields())
							.setPostFilter(FilterBuilders.termFilter("tags", tag)).get());
		}
		Object data=null;
		if (metric instanceof HistogramMetric) {
			data = ((HistogramMetric) metric).calculateHistogram(responses, buckets, start, stop, step);
		} else {
			data = metric.calculateMetric(responses, start, stop, step);
		}
		res.setContentType("application/json");
		Gson out = new Gson();
		res.getOutputStream().write(out.toJson(data).getBytes());
	}

	@Override
	public String getServletInfo() {
		return "Cluster metrics";
	}

	@Override
	public void destroy() {
		node.stop();
	}

	public static Client getClient() {
		return node.client();
	}

	private void setupElasticSearch(ServletContext context) {
		ImmutableSettings.Builder settingsBuilder = ImmutableSettings.settingsBuilder();
		String nodeName = getInitParameter(context, "node.name", getHostName());
		if (nodeName == null || nodeName.isEmpty() || "localhost".equals(nodeName)) {
			nodeName = randomNodeId();
		}
		settingsBuilder.put("node.name", nodeName);
		settingsBuilder.put("path.data", getInitParameter(context, "path.data", "data/index"));
		String httpPort = getInitParameter(context, "http.port", null);
		if (httpPort != null) {
			settingsBuilder.put("http.enabled", true);
			settingsBuilder.put("http.port", Integer.parseInt(httpPort));
		}
		this.settings = settingsBuilder.build();
		node = NodeBuilder.nodeBuilder()
				.settings(settings)
				.clusterName(getInitParameter(context, "cluster.name", "metrics"))
				.data(true).local(true).node();

	}

	public String getInitParameter(ServletContext context, String name, String defaultVal) {
		String ret = context.getInitParameter(name);
		if (ret == null || ret.isEmpty()) return defaultVal;
		return ret;
	}

	public static String getHostName() {
		Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface nic = interfaces.nextElement();
				Enumeration<InetAddress> addresses = nic.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (!address.isLoopbackAddress()) {
						return address.getHostName();
					}
				}
			}
			return "localhost";
		} catch (SocketException e) {
			return "localhost";
		}
	}

	public String randomNodeId() {
		return new BigInteger(130, random).toString(32);
	}
	
	private List<String> getIndexes(long start, long end) {
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
				String.format("%02d", startCal.get(Calendar.MONTH)) + 
				"-" + String.format("%02d", startCal.get(Calendar.DAY_OF_MONTH)));
			startCal.add(Calendar.DAY_OF_YEAR, 1);
		}
		return ret;
	}
}
