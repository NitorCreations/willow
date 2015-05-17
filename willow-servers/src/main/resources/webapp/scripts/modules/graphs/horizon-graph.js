Box.Application.addModule('horizon-graph', function(context) {
  'use strict';

  var moduleElem, configurationId,
      d3, $,
      store, windowSvc, utils, metricsService, cubismGraphs;

  var enableTerminalButton = true;
  var enableShareToRadiatorButton = true;
  var defaultColors = ["#08519c", "#3182bd", "#6baed6", "#bdd7e7", "#bae4b3", "#74c476", "#31a354", "#006d2c"];
  var cpuColors = ["#08519c", "#3182bd", "#6baed6", "#bdd7e7", "#bae4b3", "#006d2c", "#b07635", "#d01717"];
  var metricMap = {
    "cpu" : { "title" : "cpu: ", "format" : ".2f", "extent": [0, 100], colors : cpuColors, height: 50 },
    "mem" : { "title" : "mem: ", "format" : ".2f", "extent": [0, 100], colors : cpuColors, height: 50 },
    "net" : { "title" : "net: ", "format" : ".2f", "extent": undefined, colors : defaultColors, height: 50 },
    "diskio" : { "title" : "io: ", "format" : ".2f", "extent": undefined, colors : defaultColors, height: 50 },
    "tcpinfo" : { "title" : "conn: ", "format" : ".0f", "extent": undefined, colors : defaultColors, height: 50 }
  };

  function resetGraph() {
    var chartConfig = readConfiguration();
    var metricSetting = $(metricMap).attr(chartConfig.metric);

    // TODO: this should be done by reconfiguring, not destroying
    moduleElem.selectAll(".horizon").remove();

    var id = moduleElem.attr('id');
    cubismGraphs.onFocus(function(index) {
      moduleElem.selectAll(".horizon .value").style("right", index === null ? null : this.size() - index + "px");
    }, id);

    $(".horizon").unbind("mousedown");
    var chartData = metricsChart(chartConfig.metric, chartConfig.instanceTag);
    moduleElem.call(createHorizon, chartConfig.host, chartConfig.metric, chartData, metricSetting);
  }

  // graph destroy, put this on a button or such
  function removeGraph() {
    cubismGraphs.onFocus(null);
    moduleElem.select(".horizon").call(cubismGraphs.removeHorizonGraph());
    moduleElem.remove();
  }

  function readConfiguration() {
    return store.readConfiguration(configurationId);
  }

  function storeConfiguration(config) {
    store.storeConfiguration(configurationId, config);
  }

  // creating a new metrics chart every time graph is reset will not remove the old metric
  // TODO: figure out how to reset metrics, otherwise they'll keep ticking = sending requests
  function metricsChart(type, instanceTag) {
    return cubismGraphs.createMetrics(function(start, stop, step, callback) {
      var metricDataSource = metricsService.metricsDataSource(type, instanceTag, start.getTime(), stop.getTime(), step);
      metricDataSource(function(data) {
        if (data instanceof Error) {
          callback(data);
        }
        var parsedData = data.map(function(d) { return d.value; });
        callback(null, parsedData);
      });
    }, String(type));
  }

  var createHorizon = function(parentElement, host, metric, chart, metricSettings) {
    var horizonGraphElements = parentElement.selectAll(".horizon-" + host) //FIXME remove, this is currently empty selection
        .data([chart])
        .enter().append("div").classed("horizon", true);

    horizonGraphElements.call(appendHorizonGraph, host, metric, metricSettings);
    if (enableTerminalButton) {
      horizonGraphElements.call(appendTerminalIcon, host);
    }
    if (enableShareToRadiatorButton) {
      horizonGraphElements.call(appendShareRadiatorIcon, host);
    }
    horizonGraphElements.call(appendHostRadiatorLink, metricSettings.title, host);
  };

  function appendHorizonGraph(parentElement, host, metric, metricSettings) {
    return parentElement
      .attr("data-metric", metric)
      .attr("data-host", host)
      .call(configureHorizonGraph(metricSettings));
  }

  function configureHorizonGraph(metricSettings) {
    return cubismGraphs.createHorizonGraph()
      .height(metricSettings.height)
      .colors(metricSettings.colors)
      .extent(metricSettings.extent)
      .format(d3.format(metricSettings.format))
      .title(null);
  }

  function appendTerminalIcon(parentElement, host) {
    return parentElement.append("svg").attr("viewBox", "0 0 100 100")
        .classed("icon shape-terminal terminal-" + host, true)
        .attr("data-type", "start-terminal")
        .attr("data-host", host)
        .append("use").attr("xlink:href", "#shape-terminal");
  }

  function appendShareRadiatorIcon(parentElement, host) {
    return parentElement.append("svg").attr("viewBox", "0 0 100 100")
        .classed("icon shape-share share-" + host, true)
        .attr("data-type", "to-radiator")
        .append("use").attr("xlink:href", "#shape-to-radiator");
  }

  function appendHostRadiatorLink(parentElement, title, host) {
    return parentElement.append("div").classed("host-link", true)
        .text(title).append("a")
        .attr("href", "radiator.html#host=" + host)
        .attr("data-host", host)
        .attr("data-type", "host-radiator").text(host);
  }

  return {
    init: function() {
      $          = context.getGlobal("jQuery");
      d3         = context.getGlobal("d3");

      windowSvc  = context.getService("window");
      utils      = context.getService("utils");
      metricsService = context.getService("metrics");
      cubismGraphs = context.getService("cubism-graphs");
      store      = context.getService("configuration-store");

      moduleElem = d3.select(context.getElement());

      configurationId = context.getConfig('configurationIdPrefix') + moduleElem.attr('id');
      enableTerminalButton = !context.getConfig('disableTerminalButton');
      enableShareToRadiatorButton = !context.getConfig('disableRadiatorShareButton');
    },

    destroy: function() {
      removeGraph();
      moduleElem = null;
    },

    onclick: function(event, element, elementType) {
      var host = element ? element.getAttribute("data-host") : null;
      var user = element ? (element.getAttribute("data-user") ? element.getAttribute("data-user") : "@admin") : "@admin";
      switch (elementType) {
        case 'start-terminal':
          windowSvc.openTerminalToHost(user, host);
          break;
        case 'to-radiator':
          // Actually should show a menu to select radiator
          windowSvc.sendGraphToRadiator(readConfiguration(), "newradiator");
          break;
        case 'close':
          break;
        case 'host-radiator':
          windowSvc.openRadiatorForHost(host);
          event.preventDefault();
          break;
      }
    },

    messages: ["metric-changed", "reload-graph-configuration", "cubism-context-reset"],

    onmessage: function(name, data) {
      switch (name) {
        case 'metric-changed':
          this.setMetric(data);
          break;
        case 'cubism-context-reset':
        case 'reload-graph-configuration':
          resetGraph();
          break;
      }
    },

    //FIXME should these manipulations be in index level?
    setMetric: function(metric) {
      var config = readConfiguration();
      config.metric = metric;
      storeConfiguration(config);
      resetGraph();
    }
  };
});