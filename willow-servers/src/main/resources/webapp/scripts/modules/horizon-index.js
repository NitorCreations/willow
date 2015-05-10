Box.Application.addModule('horizon-index', function(context) {
  'use strict';

  var d3, moduleElem, metric, timescale, windowSvc, cubismGraphs, utils, metricsService;

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

    moduleElem.selectAll('.axis, .rule').remove();

    cubismGraphs.resetCubismContext(step, window.innerWidth);
    initLayout(window.innerWidth);
    initGraphs(metric, start, stop, step);
  }

  function initGraphs(metric, start, stop, step) {
    metricsService.hostsDataSource(metric, start, stop, step)(function(hosts) {
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

  function createHorizonGraph(parentElement, chartConfig) {
    var horizonGraphElement = parentElement.append("div")
        .attr("data-module","horizon-graph")[0][0];
    Box.Application.start(horizonGraphElement);
    localStorage.setItem(d3.select(horizonGraphElement).attr('id'), JSON.stringify(chartConfig)); //TODO this should use namespacing
    context.broadcast("reload-graph-configuration", 11);
  }

  return {
    init: function() {
      d3           = context.getGlobal("d3");
      moduleElem   = d3.select(context.getElement());

      utils        = context.getService("utils");
      windowSvc    = context.getService("window");
      cubismGraphs = context.getService("cubism-graphs");
      metricsService = context.getService("metrics");

      // TODO: configSvc for configs
      metric     = windowSvc.getHashVariable("metric") || "cpu";
      timescale    = windowSvc.getHashVariable("timescale") || 10800;

      reset();
    },

    destroy: function() {
    },

    onclick: function(event, element, elementType) {
    },

    onmessage: function(name, data) {
    }
  };
});
