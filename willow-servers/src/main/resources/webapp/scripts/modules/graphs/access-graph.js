Box.Application.addModule('access-graph', function(context) {
  'use strict';

  var nv, d3, host, windowSvc, metrics, store, utils;

  var moduleElement, moduleConf, detailsStart, detailsStop, detailsStep = 60000;

  function xTicks(d) {
    return d3.time.format('%X')(new Date(d));
  }

  function createAccessGraph(data) {
    nv.addGraph(function() {
      var chart =  nv.models.multiBarChart()
        .margin({top: 30, right: 20, bottom: 50, left: 75})
        .showControls(false)
        .stacked(true);
      chart.xAxis.tickFormat(xTicks);
      chart.yAxis.tickFormat(d3.format('0f'));
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
    moduleElement.selectAll(".nv-graph").remove();
    moduleElement
      .append("div").classed('nv-graph', true)
      .append("svg").classed('graph', true);
    metrics.metricsDataSource("access", "host_" + host, detailsStart, detailsStop, detailsStep)(createAccessGraph);
  }

  function openGraphInPopup() {
    var radiatorName = utils.guid(),
        url = '/graph.html#name=' + radiatorName;

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

      moduleConf   = context.getConfig() || {};
      moduleConf.chart = moduleConf.chart || {
        type: 'access',
        host: windowSvc.getHashVariable("host")
      };
      host = moduleConf.chart.host;
      detailsStop  = parseInt(new Date().getTime());
      detailsStart = parseInt(detailsStop - (1000 * 60 * 60 * 3));

      moduleElement.append("div").classed("nv-graph__icons", true);
      moduleElement.call(appendShareRadiatorIcon);
      moduleElement.call(appendPopupGraphIcon);

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
        case 'to-radiator':
          context.broadcast("open-radiator-list", moduleConf.chart);
          break;
      }
    },
  };
});
