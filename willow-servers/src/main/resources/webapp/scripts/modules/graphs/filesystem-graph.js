Box.Application.addModule('filesystem-graph', function(context) {
  'use strict';

  var nv, d3, host, windowSvc, metrics, moduleConf, store, utils;

  var moduleElement, detailsStop;

  var megabyte = 1024, gigabyte = 1024*1024, teratybe = 1024*1024*1024;

  var kiloBytesToString = function (kbytes) {
    var fmt = d3.format('.0f');
    if (kbytes < megabyte) {
      return fmt(kbytes) + 'kB';
    } else if (kbytes < gigabyte) {
      return fmt(kbytes / megabyte) + 'MB';
    } else if (kbytes < teratybe) {
      return fmt(kbytes / gigabyte) + 'GB';
    } else {
      return fmt(kbytes / teratybe) + 'TB';
    }
  };

  var createFsGraph = function(data) {
    nv.addGraph(function() {
      var chart = nv.models.multiBarHorizontalChart()
        .margin({top: 30, right: 20, bottom: 50, left: 75})
        .tooltips(false)
        .showControls(false)
        .stacked(true);
      chart.yAxis.tickFormat(kiloBytesToString);
      moduleElement.select(".graph").datum(data).call(chart);
      return chart;
    });
  };

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
    moduleElement.append("svg").classed("graph", true);
    metrics.metricsDataSource("disk", "host_" + host, undefined, detailsStop, undefined)(createFsGraph);
  }

  function openGraphInPopup() {
    var radiatorName = utils.guid(),
        url = '/graph.html#name=' + radiatorName;

    store.customRadiators.appendConfiguration(radiatorName, {
      chart: {
        type: 'filesystem',
        host: host
      },
      removeAfterUse: true
    });

    windowSvc.popup({
      url: url,
      height: window.innerHeight * 0.75,
      width: window.innerWidth * 0.75
    });
  }

  function openRadiatorDialog() {
    context.broadcast("open-radiator-list", moduleConf.chart);
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

      moduleConf = context.getConfig() || {};
      host       = moduleConf.chart ? moduleConf.chart.host : windowSvc.getHashVariable("host");
      moduleElement.append("div").classed("nv-graph__icons", true);
      moduleElement.call(appendShareRadiatorIcon);
      moduleElement.call(appendPopupGraphIcon);

      detailsStop = parseInt(new Date().getTime());

      reset();
    },

    messages: [ "selected-time-range-updated" ],

    onmessage: function(name, data) {
      switch (name) {
        case 'selected-time-range-updated':
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
          openRadiatorDialog();
          break;
 }
    },
  };
});
