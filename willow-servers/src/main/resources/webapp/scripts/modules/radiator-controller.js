Box.Application.addModule('radiator-controller', function(context) {
  'use strict';

  var utils, store, windowSvc, intercom, d3, $, moduleElem, cubismGraphs;

  var render = {
    horizon: function() {
      var host          = windowSvc.getHashVariable("host"),
          metric        = windowSvc.getHashVariable("metric"),
          chartConfig   = {
            metric:      metric,
            host:        host,
            instanceTag: "host_" + host
          };

      cubismGraphs.resetCubismContext();
      $(window).resize(utils.debouncer(cubismGraphs.resetCubismContext));
      moduleElem.call(createHorizonGraph, chartConfig);
      context.broadcast("reload-graph-configuration");
      windowSvc.setTitle(metric.toUpperCase() + " for " + host);
    }
  };

  function initGraph(type) {
    if (!render[type]) {
      throw new Error('Graph type not found.');
    }

    render[type]();
  }

  function radiatorGraphIdPrefix(radiatorId) {
    return "live:radiator:" + radiatorId + ":graph-";
  }

  function injectModuleConfiguration(horizonGraphElement, radiatorIdPrefix, chartConfig) {
    var radiatorConfig = {
      configurationIdPrefix: radiatorIdPrefix,
      disableTerminalButton: true,
      disableRadiatorShareButton: false,
      chart: chartConfig
    };
    utils.setConfigurationElement(horizonGraphElement, radiatorConfig);
  }

  function createHorizonGraph(parentElement, chartConfig) {
    var axisElement = parentElement.append("div")
      .attr("data-module", "horizon-ruler");

    axisElement.append("div").classed("axis", true);
    axisElement.append("div").classed("rule", true);
    Box.Application.start(axisElement[0][0]);

    var radiatorIdPrefix = radiatorGraphIdPrefix('name');
    var horizonGraphElement = parentElement.append("div")
      .attr("data-module", "horizon-graph");
    injectModuleConfiguration(horizonGraphElement, radiatorIdPrefix, chartConfig);
    Box.Application.start(horizonGraphElement[0][0]);
  }

  return {
    init: function() {
      intercom   = context.getGlobal("Intercom").getInstance();
      d3         = context.getGlobal("d3");
      $          = context.getGlobal("jQuery");
      moduleElem = d3.select(context.getElement());

      utils          = context.getService("utils");
      windowSvc      = context.getService("window");
      store          = context.getService("configuration-store");
      cubismGraphs   = context.getService("cubism-graphs");

      initGraph( windowSvc.getHashVariable("type") );
    },

    messages: ["timescale-changed"],

    onmessage: function(name, timescale) {
      switch (name) {
        case 'timescale-changed':
          cubismGraphs.resetCubismContext();
          break;
      }
    }
  };
});
