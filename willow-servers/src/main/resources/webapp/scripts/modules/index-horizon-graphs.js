Box.Application.addModule('index-horizon-graphs', function(context) {
  'use strict';

  var d3, $, moduleElem, metric, timescale,
    store, windowSvc, cubismGraphs, utils, metricsService;

  function reset() {
    var stop = new Date().getTime();
    var start = stop - (timescale * 1000);

    resetLayout();
    initGraphs(metric, start, stop);
  }

  //FIXME should get reset variables as arguments
  function resetLayout() {
    var widthInPixels = $(window).width();
    var step = parseInt(timescale * 1000 / widthInPixels);
    cubismGraphs.resetCubismContext(step, widthInPixels);
  }

  function initGraphs(metric, start, stop) {
    metricsService.hostsDataSource(metric, start, stop)(function(hosts) {
      hosts.sort();
      hosts.map(resolveHostName)
        .forEach(function (tag) {
          var chartConfig = {
            metric: metric,
            host: tag.host,
            instanceTag: tag.raw
          };
          moduleElem.select("#horizon-graphs").call(createHorizonGraph, chartConfig);
        });
      context.broadcast("reload-graph-configuration");
    });

    function resolveHostName(tag) {
      return { raw: tag, host: tag.substring(5) };
    }
  }

  function injectModuleConfiguration(horizonGraphElement, configIdPrefix) {
    var graphConfig = { configurationIdPrefix: configIdPrefix };
    utils.setConfigurationElement(horizonGraphElement, graphConfig);
  }

  function createHorizonGraph(parentElement, chartConfig) {
    var metricIdPrefix = "live:metrics:graph-";
    var horizonGraphElement = parentElement.append("div")
      .attr("data-module", "horizon-graph");
    injectModuleConfiguration(horizonGraphElement, metricIdPrefix);
    Box.Application.start(horizonGraphElement[0][0]);
    store.storeConfiguration(metricIdPrefix + horizonGraphElement.attr('id'), chartConfig); //TODO this should use namespacing
  }

  return {
    init: function() {
      d3           = context.getGlobal("d3");
      $            = context.getGlobal("jQuery");
      moduleElem   = d3.select(context.getElement());

      utils        = context.getService("utils");
      windowSvc    = context.getService("window");
      cubismGraphs = context.getService("cubism-graphs");
      metricsService = context.getService("metrics");
      store        = context.getService("configuration-store");

      // TODO: configSvc for configs
      metric     = windowSvc.getHashVariable("metric") || "cpu";
      timescale    = windowSvc.getHashVariable("timescale") || 10800;

      reset();
      $(window).resize(utils.debouncer(resetLayout));
    },

    destroy: function() {
    },

    onclick: function(event, element, elementType) {
    },

    messages: ["timescale-changed"],

    onmessage: function(name, data) {
      switch (name) {
        case 'timescale-changed':
          timescale = data;
          resetLayout();
          break;
      }
    }
  };
});
