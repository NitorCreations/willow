package com.nitorcreations.willow.metrics;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.gson.Gson;
import com.nitorcreations.willow.utils.HostUtil;

public class MetricsServlet extends GenericServlet implements Servlet {
	private static final long serialVersionUID = -6704365246281136504L;
	private static Node node;

	Map<String, Class<? extends Metric<?>>> metrics = new HashMap<>();
	ServletConfig config;
	private SecureRandom random = new SecureRandom();
	private Settings settings;

	@Override
	public void init(ServletConfig config) throws ServletException {
		this.config = config;
		metrics.put("/hosts", HostTagMetric.class);
		metrics.put("/cpu", CpuBusyMetric.class);
		metrics.put("/mem", PhysicalMemoryMetric.class);
		metrics.put("/diskio", DiskIOMetric.class);
		metrics.put("/disk", DiskStatusMetric.class);
		metrics.put("/tcpinfo", ConnectionsMetric.class);
		metrics.put("/net", NetworkMetric.class);
		metrics.put("/heap", HeapMemoryMetric.class);
		metrics.put("/requests", RequestCountMetric.class);
		metrics.put("/latency", RequestDurationMetric.class);
		metrics.put("/types", MessageTypesMetric.class);
		setupElasticSearch(config.getServletContext());
	}
	@Override
	public ServletConfig getServletConfig() {
		return config;
	}
	@Override
	public void service(ServletRequest req, ServletResponse res) throws IOException {
		if (!((HttpServletRequest)req).getMethod().equals("GET")) {
			((HttpServletResponse)res).sendError(405, "Only GET allowed");
			return;
		}
		String metricKey = ((HttpServletRequest)req).getPathInfo();
		Class<? extends Metric<?>> metricClass = metrics.get(metricKey);
		if (metricClass == null) {
			((HttpServletResponse)res).sendError(404, "Metric " + metricKey + " not found");
			return;
		}
		Metric<?> metric;
		try {
			metric = metricClass.newInstance();
			Object data = metric.calculateMetric(getClient(), (HttpServletRequest)req);
			res.setContentType("application/json");
			Gson out = new Gson();
			res.getOutputStream().write(out.toJson(data).getBytes());
		} catch (InstantiationException | IllegalAccessException e) {
			log("Failed to create metric", e);
		}
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
		String nodeName = getInitParameter(context, "node.name", HostUtil.getHostName());
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
	public String randomNodeId() {
		return new BigInteger(130, random).toString(32);
	}
	
}
