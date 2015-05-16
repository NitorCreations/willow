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

  function reset() {
    moduleElement.selectAll("svg").remove();
    moduleElement.append("svg");
    metrics.metricsDataSource("disk", "host_" + host, undefined, detailsStop, undefined)(createFsGraph);
  }

  return {
    init: function() {
      $          = context.getGlobal("jQuery");
      d3         = context.getGlobal("d3");
      nv         = context.getGlobal("nv");
      windowSvc  = context.getService("window");
      metrics    = context.getService("metrics");

      moduleElement = d3.select(context.getElement());

      host        = windowSvc.getHashVariable("host");
      detailsStop = parseInt(new Date().getTime());

      reset();
    },

    messages: [ "details-updated" ],

    onmessage: function(name, data) {
      switch (name) {
        case 'details-updated':
          reset();
          break;
      }
    }
  };
});
