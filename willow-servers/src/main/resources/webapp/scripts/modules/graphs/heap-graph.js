Box.Application.addModule('heap-graph', function(context) {
  'use strict';

  var nv, d3, host, windowSvc, metrics;

  var moduleElement, moduleConf, detailsStart, detailsStop, detailsStep = 150000;

  function xTicks(d) {
    return d3.time.format('%X')(new Date(d));
  }

  function createHeapGraph(data) {
    nv.addGraph(function() {
      var chart = nv.models.lineChart();
      chart.xAxis.tickFormat(xTicks);
      chart.yAxis.tickFormat(d3.format(',.2s'));
      moduleElement.select(".graph")
        .datum(data)
        .transition().duration(500)
        .call(chart);
      return chart;
    });
  }

  function appendPopupGraphIcon(parentElement) {
    return parentElement.select('.nv-graph__icons')
        .append("svg").attr("viewBox", "0 0 100 100")
        .classed("icon popup-" + host, true)
        .attr("data-type", "to-popup")
        .append("use").attr("xlink:href", "#shape-external-link");
  }

  function appendShareRadiatorIcon(parentElement) {
    return parentElement.select('.nv-graph__icons').append("svg").attr("viewBox", "0 0 100 100")
        .classed("icon share-" + host, true)
        .attr("data-type", "to-radiator")
        .append("use").attr("xlink:href", "#shape-to-radiator");
  }

  function reset() {
    moduleElement.selectAll(".graph").remove();
    moduleElement.append("svg").classed('graph', true);
    metrics.metricsDataSource("heap", "host_" + host, detailsStart, detailsStop, detailsStep)(createHeapGraph);
  }

  function openGraphInPopup() {
    var url = '/graph.html#type=heap&host=' + host;

    windowSvc.popup({
      url: url,
      height: window.innerHeight * 0.75,
      width: window.innerWidth * 0.75
    });
  }

  return {
    init: function() {
      d3         = context.getGlobal("d3");
      nv         = context.getGlobal("nv");
      windowSvc  = context.getService("window");
      metrics    = context.getService("metrics");

      moduleElement = d3.select(context.getElement());
      moduleElement.append("div").classed("nv-graph__icons", true);
      moduleConf = context.getConfig() || {};
      moduleElement.call(appendShareRadiatorIcon);
      moduleElement.call(appendPopupGraphIcon);

      host         = windowSvc.getHashVariable("host");
      detailsStop  = parseInt(new Date().getTime());
      detailsStart = parseInt(detailsStop - (1000 * 60 * 60 * 3));

      reset();
    },

    messages: [ "selected-time-range-updated" ],

    onmessage: function(name, data) {
      switch (name) {
        case 'selected-time-range-updated':
          detailsStart = data.start;
          detailsStop = data.stop;
          reset();
          break;
      }
    },

    onclick: function(event, element, elementType) {
      switch (elementType) {
        case 'to-popup':
          openGraphInPopup();
          break;
      }
    },

  };
});
