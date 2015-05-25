Box.Application.addModule('index-horizon-graphs', function(context) {
  'use strict';

  var d3, $, moduleElem, metric, timescale, session,
    store, windowSvc, cubismGraphs, utils, metricsService, storedHosts;
  Array.prototype.equals = function (array) {
    if (!array) {
      return false;
    }
    if (this.length != array.length) {
      return false;
    }
    for (var i = 0, l=this.length; i < l; i++) {
      if (this[i] instanceof Array && array[i] instanceof Array) {
        if (!this[i].equals(array[i])) {
          return false;
        }
      }
      else if (this[i] != array[i]) {
        return false;
      }
    }
    return true;
  };

  function reset() {
    var stop = new Date().getTime();
    var start = stop - (timescale * 1000);

    cubismGraphs.resetCubismContext();
    initGraphs(metric, start, stop);
  }

  function initGraphs(metric, start, stop) {
    metricsService.hostsDataSource(metric, start, stop)(function(hosts) {
      hosts.sort();
      storedHosts = hosts;
      hosts.map(resolveHostName)
        .forEach(function (tag) {
          var chartConfig = {
            metric: metric,
            host: tag.host,
            instanceTag: tag.raw,
            type: 'horizon'
          };
          moduleElem.select("#horizon-graphs").call(createHorizonGraph, chartConfig);
        });
      context.broadcast("reload-graph-configuration");
    });

  }
  function resolveHostName(tag) {
    return { raw: tag, host: tag.substring(5) };
  }

  function injectModuleConfiguration(horizonGraphElement, configIdPrefix, chartConfig) {
    var graphConfig = {
      configurationIdPrefix: configIdPrefix,
      disableTerminalButton: !session.isAdmin,
      chart: chartConfig
    };
    utils.setConfigurationElement(horizonGraphElement, graphConfig);
  }

  function createHorizonGraph(parentElement, chartConfig) {
    var metricIdPrefix = "live:metrics:graph-";
    var horizonGraphElement = parentElement.append("div")
      .attr("class", "horizon-module")
      .attr("data-module", "horizon-graph");
    injectModuleConfiguration(horizonGraphElement, metricIdPrefix, chartConfig);
    Box.Application.start(horizonGraphElement[0][0]);
    store.storeConfiguration(metricIdPrefix + horizonGraphElement.attr('id'), chartConfig); //TODO this should use namespacing
  }

  function checkHosts() {
    var stop = new Date().getTime();
    var start = stop - (timescale * 1000);
    metricsService.hostsDataSource(metric, start, stop)(function(hosts) {
      hosts.sort();
      if (!hosts.equals(storedHosts)) {
        var oldHosts = storedHosts;
        storedHosts = hosts.slice();
        for (var i=0; i<oldHosts.length; i++) {
          var foundIdx = hosts.indexOf(oldHosts[i]);
          if (foundIdx > -1) {
            hosts.splice(foundIdx, 1);
          }
        }
        if (hosts.length > 0) {
          hosts.map(resolveHostName)
            .forEach(function (tag) {
              var chartConfig = {
              metric: metric,
              host: tag.host,
              instanceTag: tag.raw,
              type: 'horizon'
            };
            moduleElem.select("#horizon-graphs").call(createHorizonGraph, chartConfig);
          });
          context.broadcast("reload-graph-configuration");
        }
      }
    });
  }
  return {
    init: function() {
      d3           = context.getGlobal("d3");
      $            = context.getGlobal("jQuery");
      moduleElem   = d3.select(context.getElement());
      session      = context.getGlobal("session");

      utils        = context.getService("utils");
      windowSvc    = context.getService("window");
      cubismGraphs = context.getService("cubism-graphs");
      metricsService = context.getService("metrics");
      store        = context.getService("configuration-store");

      // TODO: configSvc for configs
      metric     = windowSvc.getHashVariable("metric") || "cpu";
      timescale    = windowSvc.getHashVariable("timescale") || 10800;

      reset();
      $(window).resize(utils.debouncer(cubismGraphs.resetCubismContext));
      setInterval(checkHosts, 3000);
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
          cubismGraphs.resetCubismContext();
          break;
      }
    }
  };
});
