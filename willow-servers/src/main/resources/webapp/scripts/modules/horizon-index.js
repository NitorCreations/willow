Box.Application.addModule('horizon-index', function(context) {
  'use strict';

  var d3, moduleElem, metric, timescale, store, windowSvc, cubismGraphs, utils, metricsService;

  //TODO could these be shared with radiator index?
  function initLayout(widthInPixels) {
    moduleElem.attr("style", "width: " + widthInPixels + "px");

    moduleElem.insert("div", ":first-child")
      .classed("axis", true)
      .call(cubismGraphs.createGraphAxis().orient("top").tickFormat(d3.time.format("%H:%M")));

    moduleElem.insert("div", ":first-child")
      .classed("rule", true)
      .call(cubismGraphs.createRulerOverGraphs());
  }

  function reset() {
    var widthInPx = $(window).width();
    var step = parseInt(timescale * 1000 / widthInPx);
    var stop = new Date().getTime();
    var start = stop - (timescale * 1000);

    cubismGraphs.resetCubismContext(step, widthInPx);
    resetLayout();
    initGraphs(metric, start, stop, step);
  }

  function resetLayout() {
    moduleElem.selectAll('.axis, .rule').remove();
    initLayout($(window).width());
  }

  function initGraphs(metric, start, stop, step) {
    metricsService.hostsDataSource(metric, start, stop)(function(hosts) {
      hosts.sort();
      hosts.map(resolveHostName)
          .forEach(function (tag) {
            var chartConfig = {
              metric: metric,
              host: tag.host,
              instanceTag:tag.raw,
              stop: stop, //FIXME time related configurations should be in graph-module itself, not related to metrics itself
              step: step
            };
            moduleElem.call(createHorizonGraph, chartConfig);
          });
    });

    function resolveHostName(tag) {
      return { raw: tag, host: tag.substring(5) };
    }

    // function horizonGraphNotExists(tag) {
    //   return !$(".horizon-" + tag.host).length;
    // }
  }

  function injectModuleConfiguration(horizonGraphElement, radiatorIdPrefix) {
    var radiatorConfig = { configurationIdPrefix: radiatorIdPrefix };
    horizonGraphElement.html("<script type='text/x-config'>" + JSON.stringify(radiatorConfig) + "</script>");
  }

  function createHorizonGraph(parentElement, chartConfig) {
    var metricIdPrefix = "live:metrics:graph-";
    var horizonGraphElement = parentElement.append("div")
        .attr("data-module","horizon-graph");
    injectModuleConfiguration(horizonGraphElement, metricIdPrefix);
    Box.Application.start(horizonGraphElement[0][0]);
    store.storeConfiguration(metricIdPrefix + horizonGraphElement.attr('id'), chartConfig); //TODO this should use namespacing
    context.broadcast("reload-graph-configuration");
  }

  return {
    init: function() {
      d3           = context.getGlobal("d3");
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
