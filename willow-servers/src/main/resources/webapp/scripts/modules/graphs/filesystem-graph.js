Box.Application.addModule('filesystem-graph', function(context) {
  'use strict';

  var nv, d3, $, host, windowSvc, metrics;

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
      moduleElement.select("svg").datum(data).call(chart);
      return chart;
    });
  };

  function appendPopupGraphIcon(parentElement) {
    return parentElement.select('.nv-graph__icons').append("svg").attr("viewBox", "0 0 100 100")
        .classed("icon popup-" + host, true)
        .attr("data-type", "to-popup")
        .append("use").attr("xlink:href", "#shape-external-link");
  }

  function reset() {
    moduleElement.selectAll("svg").remove();
    moduleElement.append("svg");
    metrics.metricsDataSource("disk", "host_" + host, undefined, detailsStop, undefined)(createFsGraph);
  }

  function openGraphInPopup() {
    var url = '/graph.html#type=filesystem&host=' + host;

    windowSvc.popup({
      url: url,
      height: window.innerHeight * 0.75,
      width: window.innerWidth * 0.75
    });
  }

  return {
    init: function() {
      $          = context.getGlobal("jQuery");
      d3         = context.getGlobal("d3");
      nv         = context.getGlobal("nv");
      windowSvc  = context.getService("window");
      metrics    = context.getService("metrics");

      moduleElement = d3.select(context.getElement());

      moduleElement.append("div").classed("nv-graph__icons", true);
      moduleElement.call(appendPopupGraphIcon);

      host        = windowSvc.getHashVariable("host");
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
      }
    },
  };
});
