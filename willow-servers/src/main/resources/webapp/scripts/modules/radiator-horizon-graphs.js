Box.Application.addModule('radiator-horizon-graphs', function(context) {
  'use strict';

  var d3, $,
    store, windowSvc, cubismGraphs, utils, metricsService,
    moduleElem, radiatorName, host, instanceTag, timescale;

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
    initGraphs();
  }

  //FIXME should get reset variables as arguments
  function resetLayout() {
    var widthInPixels = $(window).width();
    var step = parseInt(timescale * 1000 / widthInPixels);
    cubismGraphs.resetCubismContext(step, widthInPixels);
  }

  function initGraphs() {
    defaultMetrics(host)(function(metrics) {
      metrics.forEach(function (metric) {
        var chartConfig = {
          metric: metric,
          host: host,
          instanceTag: instanceTag
        };
        moduleElem.call(createHorizonGraph, chartConfig);
      });
      context.broadcast("reload-graph-configuration");
    });
  }

  function radiatorGraphIdPrefix(radiatorId) {
    return "live:radiator:" + radiatorId + ":graph-";
  }

  function injectModuleConfiguration(horizonGraphElement, radiatorIdPrefix, chartConfig) {
    var radiatorConfig = {
      configurationIdPrefix: radiatorIdPrefix,
      disableTerminalButton: true,
      disableRadiatorShareButton: true,
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

  //TODO resolve from backend
  function defaultMetrics(host) {
    return function(callback) {
      callback(["cpu", "mem", "diskio", "tcpinfo"]);
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
      radiatorName = windowSvc.getHashVariable("host");
      host = windowSvc.getHashVariable("host");
      instanceTag = "host_" + host;
      if (!radiatorName) {
        console.error("failed to resolve host name for the radiator metrics");
      }
      reset();
      $(window).resize(utils.debouncer(resetLayout));
    },

    destroy: function() {
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
