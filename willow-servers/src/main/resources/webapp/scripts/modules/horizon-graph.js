Box.Application.addModule('horizon-graph', function(context) {
  'use strict';

  var moduleElem, windowSvc, d3, cubism, metric, timescale, utils, $, cubismContext;

  var defaultColors = ["#08519c", "#3182bd", "#6baed6", "#bdd7e7", "#bae4b3", "#74c476", "#31a354", "#006d2c"];
  var cpuColors = ["#08519c", "#3182bd", "#6baed6", "#bdd7e7", "#bae4b3", "#006d2c", "#b07635", "#d01717"];
  var metricMap = {
    "cpu" : { "title" : "cpu: ", "format" : ".2f", "extent": [0, 100], colors : cpuColors, height: 50 },
    "mem" : { "title" : "mem: ", "format" : ".2f", "extent": [0, 100], colors : cpuColors, height: 50 },
    "net" : { "title" : "net: ", "format" : ".2f", "extent": undefined, colors : defaultColors, height: 50 },
    "diskio" : { "title" : "io: ", "format" : ".2f", "extent": undefined, colors : defaultColors, height: 50 },
    "tcpinfo" : { "title" : "conn: ", "format" : ".0f", "extent": undefined, colors : defaultColors, height: 50 }
  };

  var resetGraphs = function() {
    var widthInPx = $(window).width();
    var step = parseInt(timescale * 1000 / widthInPx);
    var stop = new Date().getTime();
    var start = stop - (timescale * 1000);

    resetCubismContext(step, widthInPx);
    initGraphLayout(widthInPx);
    initGraphs(metric, start, stop, step);
  };

  function resetCubismContext(step, widthInPixels) {
    if (cubismContext) cubismContext.stop();
    cubismContext = cubism.context()
        .step(step)
        .size(widthInPixels)
        .start();
    cubismContext.on("focus", function(i) {
      moduleElem.selectAll(".horizon .value").style("right", i === null ? null : cubismContext.size() - i + "px");
    });
  }

  function initGraphLayout(widthInPixels) {
    moduleElem.attr("style", "width: " + widthInPixels + "px");
    moduleElem.call(function(container) {
      container.append("div")
          .classed("axis", true)
          .call(cubismContext.axis()
              .orient("top")
              .tickFormat(d3.time.format("%H:%M")));
    });
    moduleElem.call(function(container) {
      container.append("div")
          .classed("rule", true)
          .call(cubismContext.rule());
    });
  }

  // graph destroy, put this on a button or such
  function removeGraph() {
    cubismContext.on("focus", null);
    moduleElem.select(".horizon").call(cubismContext.horizon().remove);
    moduleElem.remove();
  }

  function initGraphs(metric, start, stop, step) {
    var dataUrl = "metrics/hosts" + "?start=" + start + "&stop=" + stop + "&type=" + metric;
    d3.json(dataUrl, function(hosts) {
      if (!hosts) return new Error("unable to load data");
      hosts.sort();
      hosts.map(resolveHostName)
          //.filter(horizonGraphNotExists) // TODO: this should be done someplace else. module should govern itself, not how it came to be
          .forEach(function (tag) {
            var metricSettings = $(metricMap).attr(metric);
            var chart = metricsChart(metric, tag.raw, stop, step);
            moduleElem.call(createHorizon, tag.host, chart, metricSettings);
          });
      $(".horizon").unbind("mousedown"); //FIXME should be done somewhere else
    });

    function resolveHostName(tag) {
      return { raw: tag, host: tag.substring(5) };
    }

    // function horizonGraphNotExists(tag) {
    //   return !$(".horizon-" + tag.host).length;
    // }
  }

  function metricsChart(type, instanceTag, stop, step) {
    return cubismContext.metric(function(start, stop, step, callback) {
      var dataUrl = "metrics/" + type +
          "?start=" + start.getTime() +
          "&stop=" + stop.getTime() +
          "&step=" + step +
          "&tag=" + instanceTag;
      d3.json(dataUrl, function(data) {
        if (!data) return callback(new Error("unable to load data"));
        callback(null, data.map(function(d) { return d.value; }));
      });
    }, String(type));
  }

  var createHorizon = function(parentElement, host, chart, metricSettings) {
    var horizonGraphElements = parentElement.selectAll(".horizon-" + host)
        .data([chart])
        .enter().append("div");

    horizonGraphElements.call(appendHorizonGraph, host, metricSettings);
    horizonGraphElements.call(appendTerminalIcon, host);
    horizonGraphElements.call(appendShareRadiatorIcon, host);
    horizonGraphElements.call(appendHostRadiatorLink, metricSettings.title, host);
  };

  function appendHorizonGraph(parentElement, host, metricSettings) {
    return parentElement
        .classed("horizon horizon-" + host + " horizoncpu-" + host, true)
        .attr("data-host", host)
        .call(configureHorizonGraph(host, metricSettings));
  }

  function appendTerminalIcon(parentElement, host) {
    return parentElement.append("svg").attr("viewBox", "0 0 100 100")
        .classed("icon shape-terminal terminal-" + host, true)
        .attr("data-type", "start-terminal")
        .attr("data-host", host)
        .append("use").attr("xlink:href", "#shape-terminal");
  }

  function appendShareRadiatorIcon(parentElement, host) {
    return parentElement.append("svg").attr("viewBox", "0 0 100 100")
        .classed("icon shape-share share-" + host, true)
        .attr("data-type", "to-radiator")
        .append("use").attr("xlink:href", "#shape-to-radiator");
  }

  function appendHostRadiatorLink(parentElement, title, host) {
    return parentElement.append("div").classed("host-link", true)
        .text(title).append("a")
        .attr("href", "radiator.html?host=" + host)
        .attr("data-host", host)
        .attr("data-type", "host-radiator").text(host);
  }

  function configureHorizonGraph(host, metricSettings) {
    return cubismContext.horizon()
        .height(metricSettings.height)
        .colors(metricSettings.colors)
        .extent(metricSettings.extent)
        .format(d3.format(metricSettings.format))
        .title(null);
  }

  return {
    init: function() {
      $          = context.getGlobal("jQuery");
      d3         = context.getGlobal("d3");
      windowSvc  = context.getService("window");
      utils      = context.getService("utils");
      cubism     = context.getGlobal("cubism");
      moduleElem = d3.select(context.getElement());
      metric     = windowSvc.getHashVariable("metric") || "cpu";
      timescale  = windowSvc.getHashVariable("timescale") || 10800;
      $(window).resize(utils.debouncer(resetGraphs));
      resetGraphs();
    },

    destroy: function() {
      removeGraph();
      moduleElem = null;
    },

    messages: ["timescale-changed", "metric-changed"],

    onclick: function(event, element, elementType) {
      var host = element ? element.getAttribute("data-host") : null;
      var user = element ? (element.getAttribute("data-user") ? element.getAttribute("data-user") : "${admin}") : "$admin";
      switch (elementType) {
        case 'start-terminal':
          windowSvc.openTerminalToHost(user, host);
          break;
        case 'to-radiator':
          // Actually should show a menu to select radiator
          windowSvc.sendGraphToRadiator('{"type":"horizon","host":"' + host + '","metric":"' + metric + '"}', "newradiator");
          break;
        case 'close':
          break;
        case 'host-radiator':
          windowSvc.openRadiatorForHost(host);
          event.preventDefault();
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
      resetGraphs();
    },

    setMetric: function(metricName) {
      metric = metricName;
      resetGraphs();
    }
  };
});