Box.Application.addModule('access-graph', function(context) {
  'use strict';

  var nv, d3, host, windowSvc, metrics;

  var moduleElement, detailsStart, detailsStop, detailsStep = 60000;

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
    metrics.metricsDataSource("access", "host_" + host, detailsStart, detailsStop, detailsStep)(createAccessGraph);
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

    messages: [ "selected-time-range-updated" ],

    onmessage: function(name, data) {
      switch (name) {
        case 'selected-time-range-updated':
          detailsStart = data.start;
          detailsStop = data.stop;
          reset();
          break;
      }
    }
  };
});
