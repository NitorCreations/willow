Box.Application.addModule('childcpu-graph', function(context) {
  'use strict';

  var nv, d3, host, windowSvc, metrics, store, utils;

  var moduleElement, moduleConf, detailsStart, detailsStop, detailsStep;

  function xTicks(d) {
    return d3.time.format('%X')(new Date(d));
  }
  function calculateStep(timescale) {
    var steps = $(moduleElement[0]).width() / 2;
    detailsStep = parseInt((timescale *1000) / steps);
  }
  function createChildCpuGraph(data) {
    nv.addGraph(function() {
      var chart = nv.models.lineChart();
      chart.margin({right: 25});
      chart.xAxis.tickFormat(xTicks);
      chart.yAxis.tickFormat(d3.format('%'));
      moduleElement.select(".graph")
        .datum(data)
        .transition().duration(500)
        .call(chart);
      return chart;
    });
  }

  function reset() {
    moduleElement.selectAll(".nv-graph").remove();
    moduleElement
      .append("div").classed('nv-graph', true)
      .append("svg").classed('graph', true);
    metrics.metricsDataSource("childcpu", "host_" + host, detailsStart, detailsStop, detailsStep)(createChildCpuGraph);
  }

  function openGraphInPopup() {
    var radiatorName = utils.guid(),
        url = '/radiator.html#name=' + radiatorName;

    moduleConf.removeAfterUse = true;
    store.customRadiators.appendConfiguration(radiatorName, moduleConf);
    delete moduleConf.removeAfterUse;

    windowSvc.popup({
      url: url,
      height: window.innerHeight * 0.75,
      width: window.innerWidth * 0.75
    });
  }

  return {
    init: function() {
      d3        = context.getGlobal("d3");
      nv        = context.getGlobal("nv");
      windowSvc = context.getService("window");
      metrics   = context.getService("metrics");
      store     = context.getService("configuration-store");
      utils     = context.getService("utils");

      moduleElement = d3.select(context.getElement());
      moduleElement.append("div").classed("nv-graph__icons", true);
      moduleConf = context.getConfig() || {};
      moduleConf.chart = moduleConf.chart || {
        type: 'childcpu',
        host: windowSvc.getHashVariable("host")
      };
      host = moduleConf.chart.host;
      detailsStop  = parseInt(new Date().getTime());
      var timescale = windowSvc.getTimescale();
      detailsStart = parseInt(detailsStop - (1000 * timescale));
      calculateStep(timescale);

      moduleElement.call(utils.appendShareRadiatorIcon, "nv-graph__icons", host);
      moduleElement.call(utils.appendPopupGraphIcon, "nv-graph__icons", host);
      moduleElement.call(utils.appendDraggableHandleIcon, 'nv-graph__icons');

      reset();
    },

    behaviors: [ "legend-click" ],

    messages: [ "time-range-updated", "timescale-changed" ],

    onmessage: function(name, data) {
      switch (name) {
        case 'time-range-updated':
          detailsStart = data.start;
          detailsStop = data.stop;
          reset();
          break;
        case 'timescale-changed':
          detailsStop = new Date().getTime();
          detailsStart = detailsStop - (data * 1000);
          calculateStep(data);
          reset();
          break;
      }
    },
    onclick: function(event, element, elementType) {
      switch (elementType) {
        case 'to-popup':
          openGraphInPopup();
          break;
        case 'to-radiator':
          context.broadcast("open-radiator-list", moduleConf.chart);
          break;
      }
    },

  };
});
