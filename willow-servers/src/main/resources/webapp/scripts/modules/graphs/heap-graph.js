Box.Application.addModule('heap-graph', function(context) {
  'use strict';

  var nv, d3, host, windowSvc, metrics;

  var moduleElement, detailsStart, detailsStop, detailsStep = 150000;

  function xTicks(d) {
    return d3.time.format('%X')(new Date(d));
  }

  function createHeapGraph(data) {
    nv.addGraph(function() {
      var chart = nv.models.lineChart();
      chart.xAxis.tickFormat(xTicks);
      chart.yAxis.tickFormat(d3.format(',.2s'));
      moduleElement.select("svg")
        .datum(data)
        .transition().duration(500)
        .call(chart);
      return chart;
    });
  }

  function reset() {
    moduleElement.selectAll("svg").remove();
    moduleElement.append("svg");
    metrics.metricsDataSource("heap", "host_" + host, detailsStart, detailsStop, detailsStep)(createHeapGraph);
  }

  return {
    init: function() {
      d3         = context.getGlobal("d3");
      nv         = context.getGlobal("nv");
      windowSvc  = context.getService("window");
      metrics    = context.getService("metrics");

      moduleElement = d3.select(context.getElement());

      host         = windowSvc.getHashVariable("host");
      detailsStop  = parseInt(new Date().getTime());
      detailsStart = parseInt(detailsStop - (1000 * 60 * 60 * 3));

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
