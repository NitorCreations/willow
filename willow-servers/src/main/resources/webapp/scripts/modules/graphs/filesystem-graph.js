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

  function reset() {
    removeGraph();
    moduleElement
      .append("div").classed('nv-graph', true)
      .append("svg").classed("graph", true);
    metrics.metricsDataSource("disk", "host_" + host, detailsStop - 600000, detailsStop, undefined)(createFsGraph);
  }

  function removeGraph() {
    moduleElement.selectAll(".nv-graph").remove();
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
      moduleConf.chart = moduleConf.chart || {
        type: 'filesystem',
        host: windowSvc.getHashVariable("host")
      };
      host = moduleConf.chart.host;

      var graphIconsElem = moduleElement.append("div").classed("nv-graph__icons", true);
      graphIconsElem.call(utils.appendHostRadiatorLink, moduleConf.chart.type, host);
      graphIconsElem.call(utils.appendShareRadiatorIcon, host);
      graphIconsElem.call(utils.appendPopupGraphIcon, host);
      graphIconsElem.call(utils.appendRemovalButton, moduleElement.attr('id'));
      graphIconsElem.call(utils.appendDraggableHandleIcon);

      detailsStop = parseInt(new Date().getTime());

      reset();
    },

    destroy: function() {
      moduleElement.remove();
      moduleElement = null;
    },

    behaviors: [ "legend-click" ],

    messages: [ "time-range-updated" ],

    onmessage: function(name, data) {
      switch (name) {
        case 'time-range-updated':
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
    }
  };
});
