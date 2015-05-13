Box.Application.addModule('radiator-index', function(context) {
    'use strict';

    var d3, moduleElem, radiatorName, host, instanceTag, timescale,
        store, windowSvc, cubismGraphs, utils, metricsService, isDragging = false, dragStart, detailsStart, detailsStop;

  var isDraggingMouseDown = function(e) {
    $(window).mousemove(function(e) {
        if (!isDragging) {
          isDragging = true;
          dragStart = e.pageX;
        } else {
          $(".selection").show();
          $(".selection").width(Math.abs(dragStart - e.pageX));
          var axisTop = $(".axis svg").offset().top;
          $(".selection").offset({ top: axisTop, left: Math.min(dragStart, e.pageX) });
          var height = $($(moduleElem)[0]).height() - $(".axis").height();
          $(".selection").height(height);
          detailsStart = cubismGraphs.xToTime($(".selection").offset().left);
          detailsStop = cubismGraphs.xToTime($(".selection").offset().left + $(".selection").width());
        }
    });
    $(window).mouseup(isDraggingMouseUp);
    e.stopPropagation();
    e.preventDefault();
  };
  var updateCharts = function(prefix) {
    context.broadcast("details-updated", prefix);
  };
  var updateChart = function(host, prefix) {
    var divHost = host;
    return function(data) {
            d3.select('.' + prefix  + divHost + ' svg').datum(data);
            updateCharts(prefix);
        };
  };
  var isDraggingMouseUp = function(e) {
    $(window).unbind("mousemove");
    $(window).unbind("mouseup");
    if (isDragging) {
      var element = ".details-" + host;
      d3.json("metrics/disk?tag=host_" + host+ "&stop=" + detailsStop,
              updateChart(host, "fs-"));
      d3.json("metrics/heap?tag=host_" + host + "&step=15000&start=" + detailsStart + "&stop=" + detailsStop,
              updateChart(host, "heap-"));
      d3.json("metrics/access?tag=host_" + host + "&step=15000&start=" + detailsStart + "&stop=" + detailsStop,
              updateChart(host, "access-"));
    } else {
      $(".selection").hide();
      var stop = new Date().getTime();
      var start = parseInt(stop - (1000 * 60 * 60 * 3));
      d3.json("metrics/disk?tag=host_" + host + "&stop=" + stop, updateChart(host, "fs-"));
      d3.json("metrics/heap?tag=host_" + host + "&step=15000&start=" + start + "&stop=" + stop, updateChart(host, "heap-"));
      d3.json("metrics/access?tag=host_" + host + "&step=60000&start=" + start + "&stop=" + stop, updateChart(host, "access-"));

    }
    isDragging = false;
    e.stopPropagation();
    e.preventDefault();
  };
    //TODO could these be shared with horizon index?
    function initLayout(widthInPixels) {
        moduleElem.attr("style", "width: " + widthInPixels + "px");

        moduleElem.insert("div", ":first-child")
            .classed("axis", true)
            .call(cubismGraphs.createGraphAxis().orient("top").tickFormat(d3.time.format("%H:%M")));

        moduleElem.insert("div", ":first-child")
            .classed("rule", true)
            .call(cubismGraphs.createRulerOverGraphs());
        var height = $($(moduleElem)[0]).height() - $(".axis").height();
        $(".rule").height(height);
    }

    function reset() {
        var widthInPx = $(window).width();
        var step = parseInt(timescale * 1000 / widthInPx);
        var stop = new Date().getTime();

        cubismGraphs.resetCubismContext(step, widthInPx);
        resetLayout();
        initGraphs(stop, step);
    }

    function resetLayout() {
        moduleElem.selectAll('.axis, .rule').remove();
        initLayout($(window).width());
    }

    function initGraphs(stop, step) {
        defaultMetrics(host)(function(metrics) {
            metrics.forEach(function (metric) {
                var chartConfig = {
                    metric: metric,
                    host: host,
                    instanceTag: instanceTag,
                    stop: stop, //FIXME time related configurations should be in graph-module itself, not related to metrics itself
                    step: step
                };
                moduleElem.call(createHorizonGraph, chartConfig);
            });
            context.broadcast("reload-graph-configuration");
        });
    }

    function radiatorGraphIdPrefix(radiatorId) {
        return "live:radiator:" + radiatorId + ":graph-";
    }

    function injectModuleConfiguration(horizonGraphElement, radiatorIdPrefix) {
      var radiatorConfig = {
        configurationIdPrefix: radiatorIdPrefix,
        disableTerminalButton: true,
        disableRadiatorShareButton: true
      };
      utils.setConfigurationElement(horizonGraphElement, radiatorConfig);
    }

    function createHorizonGraph(parentElement, chartConfig) {
        var radiatorIdPrefix = radiatorGraphIdPrefix(radiatorName);
        var horizonGraphElement = parentElement.append("div")
            .attr("data-module","horizon-graph");
        injectModuleConfiguration(horizonGraphElement, radiatorIdPrefix);
        Box.Application.start(horizonGraphElement[0][0]);
        store.storeConfiguration(radiatorIdPrefix + horizonGraphElement.attr('id'), chartConfig); //TODO this should use namespacing
    }

    //TODO resolve from backend
    function defaultMetrics(host) {
        return function(callback) {
            callback(["cpu", "mem", "diskio", "tcpinfo"]);
        };
    }

    return {
        init: function() {
            d3           = context.getGlobal("d3");
            moduleElem   = d3.select(context.getElement());

            utils        = context.getService("utils");
            windowSvc    = context.getService("window");
            cubismGraphs = context.getService("cubism-graphs");
            metricsService = context.getService("metrics");
            store        = context.getService("configuration-store");

            // TODO: configSvc for configs
            timescale    = windowSvc.getHashVariable("timescale") || 10800;
            radiatorName = windowSvc.getHashVariable("host");
            host = windowSvc.getHashVariable("host");
            instanceTag = "host_" + host;
            if (!radiatorName) {
                console.error("failed to resolve host name for the radiator metrics");
            }
            reset();
            $(window).resize(utils.debouncer(resetLayout));
        },

        destroy: function() {
        },
        onmousedown: isDraggingMouseDown,
        onclick: function(event, element, elementType) {
        },

        messages: ["timescale-changed"],

        onmessage: function(name, data) {
            switch (name) {
                case 'timescale-changed':
                    timescale = data;
                    resetLayout();
                    break;
            }
        }
    };
});
