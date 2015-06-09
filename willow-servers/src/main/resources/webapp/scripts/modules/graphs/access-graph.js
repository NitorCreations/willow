Box.Application.addModule('access-graph', function(context) {
  'use strict';

  var nv, d3, host, windowSvc, metrics, store, utils, $;
  var minute = 60000, tenMinutes = minute * 10, hour = tenMinutes * 6, twoHours = hour * 2;
  var scales = [ { legend: "req/min", step: minute }, { legend: "req/10min", step: tenMinutes},
    { legend: "req/h", step: hour }, { legend: "req/2h", step: twoHours } ];
  var legend = "req/min";
  var moduleElement, moduleConf, isTimescaleLongerThanDay, detailsStart, detailsStop, detailsStep = 60000;

  function xTicks(d) {
    var date = new Date(d);
    return isTimescaleLongerThanDay ? utils.dayTimeFormat(date) : utils.timeFormat(date);
  }

  function calculateStep(timescale) {
    var steps = timescale * 1000;
    var i = 0;
    while (steps > ($(moduleElement[0]).width() / 2)) {
      detailsStep = scales[i].step;
      legend = scales[i].legend;
      steps = parseInt((timescale * 1000) / detailsStep);
      i++;
    }
  }

  function createAccessGraph(data) {
    nv.addGraph(function() {
      var chart =  nv.models.multiBarChart()
        .margin({top: 30, right: 20, bottom: 50, left: 75})
        .showControls(false)
        .stacked(true)
        .showLegend(true);
      chart.xAxis.tickFormat(xTicks);
      chart.yAxis.tickFormat(d3.format('0f'));
      moduleElement.select(".graph")
        .datum(data)
        .transition().duration(500)
        .call(chart);
      moduleElement.select(".graph")
        .append("text")
        .attr("x", 80)
        .attr("y", 35)
        .attr("text-anchor", "left")
        .text(legend);
      return chart;
    });
  }

  function reset() {
    isTimescaleLongerThanDay = windowSvc.getTimescale() > 86400;
    removeGraph();
    moduleElement
      .append("div").classed('nv-graph', true)
      .append("svg").classed('graph', true);
    metrics.metricsDataSource("access", "host_" + host, detailsStart, detailsStop, detailsStep)(createAccessGraph);
  }

  function removeGraph() {
    moduleElement.selectAll(".nv-graph").remove();
  }

  function openGraphInPopup() {
    var radiatorName = utils.guid(),
        url = 'radiator.html#name=' + radiatorName;

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
      $         = context.getGlobal("jQuery");

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
      var timescale = windowSvc.getTimescale();
      detailsStart = parseInt(detailsStop - (1000 * timescale));
      calculateStep(timescale);

      var graphIconsElem = moduleElement.append("div").classed("nv-graph__icons", true);
      graphIconsElem.call(utils.appendHostRadiatorLink, moduleConf.chart.type, host);
      graphIconsElem.call(utils.appendShareRadiatorIcon, host);
      graphIconsElem.call(utils.appendPopupGraphIcon, host);
      graphIconsElem.call(utils.appendRemovalButton, moduleElement.attr('id'));
      graphIconsElem.call(utils.appendDraggableHandleIcon);

      reset();
    },

    destroy: function() {
      moduleElement.remove();
      moduleElement = null;
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
    }
  };
});
