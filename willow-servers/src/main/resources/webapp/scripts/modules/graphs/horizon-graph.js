Box.Application.addModule('horizon-graph', function(context) {
  'use strict';

  var moduleElem, moduleConf, horizon = { remove: function() { return this; }},
      d3, $,
      store, windowSvc, utils, metricsService, cubismGraphs,
      initDone = false,
      messageQueue = [];
  var kilobyte = 1024, megabyte = 1024*1024, gigabyte = 1024*1024*1024, teratybe = 1024*1024*1024*1024;
  var bytesToString = function (bytes) { //FIXME to utils
    var fmt = d3.format('.0f');
    if (bytes < kilobyte) {
      return fmt(bytes) + 'B/s';
    } else if (bytes < megabyte) {
      return fmt(bytes /kilobyte) + 'kB/s';
    } else if (bytes < gigabyte) {
      return fmt(bytes / megabyte) + 'MB/s';
    } else if (bytes < teratybe) {
      return fmt(bytes / gigabyte) + 'GB/s';
    } else {
      return fmt(bytes / teratybe) + 'TB/s';
    }
  };

  var cpuColors = ["#1B5E20", "#388E3C", "#4CAF50", "#81C784","#C8E6C9","#DCEDC8","#AED581","#8BC34A","#689F38","#33691E"];
  var memColors = ["#FF6F00","#FFA000","#FFC107","#FFD54F","#FFECB3","#FFE0B2","#FFB74D","#FF9800","#F57C00","#E65100"];
  var netColors = ["#1A237E","#303F9F","#3F51B5","#7986CB","#C5CAE9","#BBDEFB","#64B5F6","#2196F3","#1976D2","#0D47A1"];
  var ioColors = ["#006064","#0097A7","#00BCD4","#4DD0E1","#B2EBF2","#B2DFDB","#4DB6AC","#009688","#00796B","#004D40"];
  var connectionsColors = ["#4A148C","#7B1FA2","#9C27B0","#BA68C8","#E1BEE7","#D1C4E9","#9575CD","#673AB7","#512DA8","#311B92"];

  var metricMap = {
    "cpu" : { "title" : "cpu", "format" : ".2f", "extent": [0, 100], colors : cpuColors, height: 50 },
    "mem" : { "title" : "mem", "format" : ".2f", "extent": [0, 100], colors : memColors, height: 50 },
    "net" : { "title" : "net", "format" : bytesToString, "extent": undefined, colors : netColors, height: 50 },
    "diskio" : { "title" : "io", "format" : bytesToString, "extent": undefined, colors : ioColors, height: 50 },
    "tcpinfo" : { "title" : "conn", "format" : ".0f", "extent": undefined, colors : connectionsColors, height: 50 }
  };

  function resetGraph() {
    var chartConfig = moduleConf.chart;
    var metricSetting = $(metricMap).attr(chartConfig.metric);

    // TODO: this should be done by reconfiguring, not destroying
    moduleElem.select(".horizon").call(horizon.remove).remove();

    var id = moduleElem.attr('id');
    cubismGraphs.onFocus(function(index) {
      moduleElem.selectAll(".horizon .value").style("right", index === null ? null : this.size() - index + "px");
    }, id);

    $(".horizon").unbind("mousedown");
    var chartData = metricsChart(chartConfig.metric, chartConfig.instanceTag);
    moduleElem.call(createHorizon, chartConfig.host, chartConfig.metric, chartData, metricSetting);
  }


  function removeGraph() {
    cubismGraphs.removeOnFocus(moduleElem.attr('id'));
    moduleElem.select(".horizon").call(horizon.remove);
    moduleElem.remove();
  }

  function storeConfiguration(config) {
    store.storeConfiguration(moduleConf.configurationId, config);
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

    var graphIconsElem = horizonGraphElements.append("div").classed('horizon__icons', true);

    horizonGraphElements.call(appendHorizonGraph, host, metric, metricSettings);
    if (!moduleConf.disableTerminalButton) {
      graphIconsElem.call(utils.appendTerminalIcon, host);
    }
    if (!moduleConf.disableRadiatorShareButton) {
      graphIconsElem.call(utils.appendShareRadiatorIcon, host);
    }

    graphIconsElem.call(utils.appendPopupGraphIcon, host);
    graphIconsElem.call(utils.appendRemovalButton, moduleElem.attr('id'));
    graphIconsElem.call(utils.appendDraggableHandleIcon);
    horizonGraphElements.call(utils.appendHostRadiatorLink, metricSettings.title, host);
  };

  function appendHorizonGraph(parentElement, host, metric, metricSettings) {
    return parentElement
      .attr("data-metric", metric)
      .attr("data-host", host)
      .call(configureHorizonGraph(metricSettings));
  }

  function configureHorizonGraph(metricSettings) {
    horizon = cubismGraphs.createHorizonGraph()
      .height(metricSettings.height)
      .colors(metricSettings.colors)
      .extent(metricSettings.extent)
      .title(null);
    if (typeof metricSettings.format == "function") {
      horizon.format(metricSettings.format);
    } else {
      horizon.format(d3.format(metricSettings.format));
    }
    return horizon;
  }

  function openGraphInPopup() {
    var radiatorName = utils.guid(),
        url = '/radiator.html#name=' + radiatorName;

    moduleConf.removeAfterUse = true;
    store.customRadiators.appendConfiguration(radiatorName, moduleConf);
    delete moduleConf.removeAfterUse; // TODO: clone the object instead of adding and removing prop

    windowSvc.popup({
      url: url
    });
  }

  //FIXME should these manipulations be in index level?
  function setMetric(metric) {
    var config = moduleConf.chart;
    config.metric = metric;
    storeConfiguration(config);
    resetGraph();
  }

  function openRadiatorDialog() {
    context.broadcast("open-radiator-list", moduleConf.chart);
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
      moduleConf = context.getConfig() || {};
      moduleConf.configurationId = moduleConf.configurationIdPrefix + moduleElem.attr('id');

      initDone = true;
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
          openRadiatorDialog();
          break;
        case 'to-popup':
          openGraphInPopup();
          break;
        case 'host-radiator':
          windowSvc.openRadiatorForHost(host);
          event.preventDefault();
          break;
      }
    },

    messages: [
      "metric-changed",
      "reload-graph-configuration",
      "cubism-context-reset",
      "time-range-selected",
      "time-range-deselected"
    ],

    onmessage: function(name, data) {
      switch (name) {
        case 'metric-changed':
          this.setMetric(data);
          break;
        case 'cubism-context-reset':
        case 'reload-graph-configuration':
          this.resetGraph();
          break;
        case 'time-range-deselected':
          // TODO: update horizon range
          cubismGraphs.start();
          break;
        case 'time-range-selected':
          // TODO: update horizon range
          cubismGraphs.stop();
          break;
      }
    },
    setMetric: setMetric,
    resetGraph: resetGraph,
    openRadiatorDialog: openRadiatorDialog,
    openGraphInPopup: openGraphInPopup
  };
});
