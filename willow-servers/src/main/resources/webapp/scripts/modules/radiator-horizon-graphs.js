Box.Application.addModule('radiator-horizon-graphs', function(context) {
  'use strict';

  var d3, $,
    store, windowSvc, cubismGraphs, utils, metricsService,
    moduleElem, radiatorName, host, timescale;

  var detailsStart, detailsStop, dragStart, isDragging = false; //FIXME can usage of these be removed?

  function timeRangeSelectionArea() {
    return $("#time-range-selection");
  }

  var isDraggingMouseDown = function(e) {
    e.stopPropagation();
    e.preventDefault();
    var selectionArea = timeRangeSelectionArea;
    $(window).mousemove(function(e) {
      if (!isDragging) {
        isDragging = true;
        dragStart = e.pageX;
      } else {
        selectionArea().show();
        selectionArea().width(Math.abs(dragStart - e.pageX));
        var axisTop = $(".axis svg").offset().top;
        selectionArea().offset({ top: axisTop, left: Math.min(dragStart, e.pageX) });
        var height = $($(moduleElem)[0]).height() - $(".axis").height();
        //selectionArea().height(height); //FIXME height calculation doesn't work.
        detailsStart = cubismGraphs.xToTime(selectionArea().offset().left);
        detailsStop = cubismGraphs.xToTime(selectionArea().offset().left + selectionArea().width());
      }
    });
    $(window).mouseup(isDraggingMouseUp);
  };

  var isDraggingMouseUp = function(e) {
    e.stopPropagation();
    e.preventDefault();
    $(window).unbind("mousemove");
    $(window).unbind("mouseup");
    if (isDragging) {
      context.broadcast("selected-time-range-updated", {start: detailsStart, stop: detailsStop});
    } else {
      timeRangeSelectionArea().hide();
      var stop = new Date().getTime();
      var start = parseInt(stop - (1000 * 60 * 60 * 3));
      context.broadcast("selected-time-range-updated", {start: start, stop: stop});
    }
    isDragging = false;
  };

  function reset() {
    resetLayout();
    var configDataSource = host ? defaultMetrics(host) : customRadiatorConfig(radiatorName);
    initGraphs(configDataSource);
  }

  //FIXME should get reset variables as arguments
  function resetLayout() {
    var widthInPixels = $(window).width();
    var step = parseInt(timescale * 1000 / widthInPixels);
    cubismGraphs.resetCubismContext(step, widthInPixels);
  }

  function initGraphs(configDataSource) {
    configDataSource(function(configurations) {
      configurations.forEach(function(chartConfig) {
        moduleElem.call(createHorizonGraph, chartConfig);
      });
    });
    context.broadcast("reload-graph-configuration");
  }

  //FIXME sami 21.5. should we remove these local storage stores now that chart config is passed through module config
  function radiatorGraphIdPrefix(radiatorId) {
    return "live:radiator:" + radiatorId + ":graph-";
  }

  function injectModuleConfiguration(horizonGraphElement, radiatorIdPrefix, chartConfig) {
    var radiatorConfig = {
      configurationIdPrefix: radiatorIdPrefix, //FIXME sami 21.5. remove, deprecated
      disableTerminalButton: true,
      disableRadiatorShareButton: false,
      chart: chartConfig
    };
    utils.setConfigurationElement(horizonGraphElement, radiatorConfig);
  }

  function createHorizonGraph(parentElement, chartConfig) {
    var radiatorIdPrefix = radiatorGraphIdPrefix(radiatorName);
    var horizonGraphElement = parentElement.append("div")
      .attr("data-module","horizon-graph");
    injectModuleConfiguration(horizonGraphElement, radiatorIdPrefix, chartConfig);
    Box.Application.start(horizonGraphElement[0][0]);
    store.storeConfiguration(radiatorIdPrefix + horizonGraphElement.attr('id'), chartConfig); //TODO this should use namespacing
  }

  function customRadiatorConfig(radiatorName) {
    return function(callback) {
      callback(store.customRadiators.readConfiguration(radiatorName));
    };
  }

  //TODO resolve from backend
  function defaultMetrics(host) {
    var defaults = ["cpu", "mem", "diskio", "tcpinfo"];
    var configs = defaults.map(function(metric) {
        return {
          metric: metric,
          host: host,
          instanceTag: "host_" + host,
          type: "horizon"
        };
      }
    );
    return function(callback) {
      callback(configs);
    };
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
      timescale    = windowSvc.getHashVariable("timescale") || 10800;

      //TODO: host hash variable is dominant over name variable
      host = windowSvc.getHashVariable("host");
      radiatorName = host || windowSvc.getHashVariable("name");

      if (!radiatorName) {
        console.error("failed to resolve host name for the radiator metrics");
      }

      reset();
      $(window).resize(utils.debouncer(resetLayout));
    },

    destroy: function() {
      timescale = null;
      host = null;
      radiatorName = null;
    },

    onmousedown: isDraggingMouseDown,

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
