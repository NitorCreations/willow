Box.Application.addModule('horizon-index', function(context) { //FIXME rename to cloud|summary|?|-index?
  'use strict';

  var moduleElement, windowSvc, d3, cubism, metric, timescale, utils, $, cubismContext, graphEnd, stop;

  var defaultColors = ["#08519c", "#3182bd", "#6baed6", "#bdd7e7", "#bae4b3", "#74c476", "#31a354", "#006d2c"];
  var cpuColors = ["#08519c", "#3182bd", "#6baed6", "#bdd7e7", "#bae4b3", "#006d2c", "#b07635", "#d01717"];
  var metricMap = {
    "cpu" : { "title" : "cpu: ", "format" : ".2f", "extent": [0, 100], colors : cpuColors, height: 50 },
    "mem" : { "title" : "mem: ", "format" : ".2f", "extent": [0, 100], colors : cpuColors, height: 50 },
    "net" : { "title" : "net: ", "format" : ".2f", "extent": undefined, colors : defaultColors, height: 50 },
    "diskio" : { "title" : "io: ", "format" : ".2f", "extent": undefined, colors : defaultColors, height: 50 },
    "tcpinfo" : { "title" : "conn: ", "format" : ".0f", "extent": undefined, colors : defaultColors, height: 50 }
  };

  var deployer_metric = function(name, tag, stop, step) {
    var hostTag = tag;
    return cubismContext.metric(function(start, stop, step, callback) {
      d3.json("metrics/" + name +
      "?start=" + start.getTime() +
      "&stop=" + stop.getTime() +
      "&step=" + step + "&tag=" + hostTag, function(data) {
        if (!data) return callback(new Error("unable to load data"));
        callback(null, data.map(function(d) { return d.value; }));
      });
    }, name += "");
  };

  var stateInHash = function() {
    var hash = window.location.hash ? window.location.hash.substring(1) : "";
    hash = utils.addOrReplaceUrlVariable(utils.addOrReplaceUrlVariable(hash, "metric", metric), "timescale", timescale);
    windowSvc.setHash(hash); //FIXME could this information be passed somehow differently to module?
    $(".nav .container a").each(function(index, element) {
      $(element).attr("class", "");
      $(element).attr("href", "#" + utils.addOrReplaceUrlVariable(hash, "metric", element.getAttribute("data-metric")));
    });
    $("#" + metric).attr("class", "pagename current");
    if ($(window).width() < 500) {
      $("#" + metric).prependTo(".nav .container");
    }
  };

  var resetGraphs = function() {
    stop = new Date().getTime();
    if (graphEnd) stop = graphEnd;
    var start = stop - (timescale * 1000);
    var size = $(window).width();
    var step = parseInt(timescale * 1000 / size);
    if (cubismContext) cubismContext.stop();
    cubismContext = cubism.context()
        .step(step)
        .size(size)
        .start();
    d3.selectAll(".horizon").call(cubismContext.horizon().remove).remove();
    d3.selectAll(".axis").remove();
    d3.selectAll(".rule").remove();
    d3.select("#chart").attr("style", "width: " + size + "px");
    d3.select("#chart").call(function(div) {
      div.append("div")
          .attr("class", "axis")
          .call(cubismContext.axis()
              .orient("top")
              .tickFormat(d3.time.format("%H:%M")));
    });
    initGraphs();
    cubismContext.on("focus", function(i) {
      d3.selectAll(".horizon .value").style("right", i === null ? null : cubismContext.size() - i + "px");
    });
    d3.select("#chart").call(function(div) {
      div.append("div")
          .attr("class", "rule")
          .call(cubismContext.rule());
    });
  };

  var initGraphs = function() {
    var step = parseInt((timescale * 1000) / $(window).width());
    var start = stop - (timescale * 1000);
    d3.json("metrics/hosts" +
    "?start=" + start +
    "&stop=" + stop +
    "&type=" + metric, function(data) {
      $(".horizon").unbind("mousedown");
      data.sort();
      if (!data) return new Error("unable to load data");
      for (var i=0; i<data.length; i++) {
        var host = data[i].substring(5);
        if ( ! $(".horizon-" + host).length ) {
          var metricSettings = $(metricMap).attr(metric);
          var chart = deployer_metric(metric, data[i], stop, step);
          d3.select("#chart").call(createHorizon, host, chart, metricSettings);
        }
      }
    });
  };

  var createHorizon = function(parentElement, host, chart, metricSettings) {
    var horizonGraphElements = parentElement.selectAll(".horizon-" + host)
        .data([chart])
        .enter().append("div");

    horizonGraphElements.call(appendHorizonGraph, host, metricSettings);
    horizonGraphElements.call(appendTerminalIcon, host);
    horizonGraphElements.call(appendShareRadiatorIcon, host);

    parentElement.call(appendDetailsScreen, host);
  };

  function appendHorizonGraph(parentElement, host, metricSettings) {
    return parentElement
        .classed("horizon horizon-" + host + " horizoncpu-" + host, true)
        .attr("data-host", host)
        .call(configureHorizonGraph(host, metricSettings));
  }

  function appendTerminalIcon(parentElement, host) {
    return parentElement.append("svg").attr("viewBox", "0 0 124 124")
        .classed("icon shape-terminal terminal-" + host, true)
        .attr("data-type", "start-terminal")
        .attr("data-host", host)
        .append("use").attr("xlink:href", "#shape-terminal");
  }

  function appendShareRadiatorIcon(parentElement, host) {
    return parentElement.append("svg").attr("viewBox", "0 0 100 100")
        .classed("icon shape-share share-" + host, true)
        .append("use").attr("xlink:href", "#shape-to-radiator");
  }

  function appendDetailsScreen(parentElement, host) {
    return parentElement.append("div").classed("details details-" + host, true);
  }

  function configureHorizonGraph(host, metricSettings) {
    return cubismContext.horizon()
        .height(metricSettings.height)
        .colors(metricSettings.colors)
        .extent(metricSettings.extent)
        .format(d3.format(metricSettings.format))
        .title(metricSettings.title + host);
  }

  return {
    init: function() {
      moduleElement = context.getElement();
      windowSvc = context.getService("window");
      utils = context.getService("utils");
      d3 = context.getGlobal("d3");
      cubism = context.getGlobal("cubism");
      $ = context.getGlobal("jQuery");

      var hash = window.location.hash ? window.location.hash.substring(1) : "";

      metric = utils.getUrlVariable(hash, metric) || "cpu";
      timescale = utils.getUrlVariable(hash, timescale) || 10800;

      stateInHash();
      resetGraphs();
    },

    destroy: function() {
      moduleElement = null;
    },

    messages: ["timescale-changed", "metric-changed"],

    onclick: function(event, element, elementType) {
      var host = element ? element.getAttribute("data-host") : null;
      switch (elementType) {
        case 'start-terminal':
          windowSvc.openTerminalToHost("pasi", host); //FIXME paratemetize user
          break;
        case 'to-radiator':
          windowSvc.openRadiatorForHost(host);
          break;
        case 'close':
          break;
      }
    },

    onmessage: function(name, data) {
      switch (name) {
        case 'timescale-changed':
          this.setTimescale(data);
          break;
        case 'metric-changed':
          this.setMetric(data);
          break;
      }
    },

    setTimescale: function(scale) {
      timescale = scale;
      stateInHash();
      resetGraphs();
    },

    setMetric: function(metricName) {
      metric = metricName;
      stateInHash();
      resetGraphs();
    }
  };
}); 