package com.nitorcreations.willow.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.nitorcreations.willow.utils.PropertyMerge;

public class PropertyServlet implements Servlet {
	ServletConfig config;
	@Override
	public void init(ServletConfig config) throws ServletException {
		this.config = config;
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
		String rootProperties = ((HttpServletRequest)req).getPathInfo();
		PropertyMerge mrg = null;
		if (config.getInitParameter("property.roots") != null) {
			String roots = config.getInitParameter("property.roots");
			if (!roots.isEmpty()) {
				mrg = new PropertyMerge(roots.split("\\|"));
			} else {
				mrg = new PropertyMerge();
			}
		}
		Properties seed = new Properties();
		for (Entry<String, String[]> next : ((HttpServletRequest)req).getParameterMap().entrySet()) {
			seed.setProperty(next.getKey(), StringUtils.join(next.getValue(), ","));
		}
		res.setContentType("text/plain;charset=utf-8");
		((HttpServletResponse)res).setStatus(200);
		Properties result = mrg.merge(seed, rootProperties);
		OutputStream out = res.getOutputStream();
		result.store(out, null);
		out.flush();
		out.close();
	}

	@Override
	public String getServletInfo() {
		return "PropertiesServlet";
	}

	@Override
	public void destroy() {
	}

}
