Box.Application.addModule('radiator-controller', function(context) {
  'use strict';

  var utils, store, windowSvc, metrics, intercom, d3, $, moduleElem, cubismGraphs,
    radiatorName, host, threaddumpChildren, configMap = {};

  var detailsStart, detailsStop, dragStart,
    isDragging = false, //FIXME can usage of these be removed?
    isTimeRangeSelected = false;

  var render = {
    horizon: function(config) {
      cubismGraphs.resetCubismContext();
      moduleElem.call(createHorizonGraph, config.chart);
      context.broadcast("reload-graph-configuration");
    },
    access: function(config) {
      moduleElem.call(createAccessGraph, config.chart);
    },
    filesystem: function(config) {
      moduleElem.call(createFilesystemGraph, config.chart);
    },
    heap: function(config) {
      moduleElem.call(createHeapGraph, config.chart);
    },
    childcpu: function(config) {
      moduleElem.call(createChildCpuGraph, config.chart);
    },
    flame: function(config) {
      if (!threaddumpChildren.length) {
        return;
      }

      threaddumpChildren.forEach(function(childtag) {
        var extendedChart = jQuery.extend({}, config.chart);
        extendedChart.childtag = childtag;
        moduleElem.call(createFlameGraph, extendedChart);
      });
    }
  };

  function initGraph(config) {
    // the config structure is a little different when it comes through custom radiator
    var type = config.chart.type;
    if (!render[type]) {
      throw new Error('Graph type not found.');
    }

    render[type](config);

    // re-render the graph on resize
    $(window).resize(utils.debouncer(function() {
      render[type](config);
    }));
  }

  function radiatorGraphIdPrefix(radiatorId) {
    return "live:radiator:" + radiatorId + ":graph-";
  }

  function injectModuleConfiguration(graphElement, radiatorIdPrefix, chartConfig) {
    var radiatorConfig = {
      configurationIdPrefix: radiatorIdPrefix,
      disableTerminalButton: true,
      disableRadiatorShareButton: false,
      chart: chartConfig
    };
    utils.setConfigurationElement(graphElement, radiatorConfig);
  }

  function createAxisRuler(parentElement) {
    var axisElement = parentElement.append("div")
      .classed("horizon-ruler", true)
      .attr("data-module", "horizon-ruler");

    axisElement.append("div").classed("axis", true);
    axisElement.append("div").classed("rule", true);
    axisElement.append("div").attr("id", "time-range-selection");
    Box.Application.start(axisElement[0][0]);
  }

  function createHorizonGraph(parentElement, chartConfig) {
    if (!parentElement.selectAll('.axis, .rule')[0].length)
      createAxisRuler(parentElement);

    var horizonGraphElement = parentElement.append("div")
      .classed("col c12", true)
      .attr("data-module", "horizon-graph");
    injectModuleConfiguration(horizonGraphElement, radiatorGraphIdPrefix('custom'), chartConfig);
    Box.Application.start(horizonGraphElement[0][0]);
    configMap[horizonGraphElement.attr('id')] = chartConfig;
  }

  function createAccessGraph(parentElement, chartConfig) {
    var accessGraphElement = parentElement.append("div")
      .classed("nv-graph__wrapper scalable col c6", true)
      .attr("data-module", "access-graph");
    injectModuleConfiguration(accessGraphElement, radiatorGraphIdPrefix('custom'), chartConfig);
    Box.Application.start(accessGraphElement[0][0]);
    configMap[accessGraphElement.attr('id')] = chartConfig;
  }

  function createFilesystemGraph(parentElement, chartConfig) {
    var filesystemGraphElement = parentElement.append("div")
      .classed("nv-graph__wrapper scalable col c6", true)
      .attr("data-module", "filesystem-graph");
    injectModuleConfiguration(filesystemGraphElement, radiatorGraphIdPrefix('custom'), chartConfig);
    Box.Application.start(filesystemGraphElement[0][0]);
    configMap[filesystemGraphElement.attr('id')] = chartConfig;
  }

  function createHeapGraph(parentElement, chartConfig) {
    var heapGraphElement = parentElement.append("div")
      .classed("nv-graph__wrapper scalable col c6", true)
      .attr("data-module", "heap-graph");
    injectModuleConfiguration(heapGraphElement, radiatorGraphIdPrefix('custom'), chartConfig);
    Box.Application.start(heapGraphElement[0][0]);
    configMap[heapGraphElement.attr('id')] = chartConfig;
  }

  function createChildCpuGraph(parentElement, chartConfig) {
    var heapGraphElement = parentElement.append("div")
      .classed("nv-graph__wrapper scalable col c6", true)
      .attr("data-module", "childcpu-graph");
    injectModuleConfiguration(heapGraphElement, radiatorGraphIdPrefix('custom'), chartConfig);
    Box.Application.start(heapGraphElement[0][0]);
    configMap[heapGraphElement.attr('id')] = chartConfig;
  }

  function createFlameGraph(parentElement, chartConfig) {
    var flameGraphElement = parentElement.append("div")
      .classed("flame-graph__wrapper scalable col c6", true)
      .attr("data-module", "flame-graph");
    injectModuleConfiguration(flameGraphElement, radiatorGraphIdPrefix('custom'), chartConfig);
    Box.Application.start(flameGraphElement[0][0]);
    configMap[flameGraphElement.attr('id')] = chartConfig;
  }

  function timeRangeSelectionArea() {
    return $("#time-range-selection");
  }

  function attachSorting() {
    $(moduleElem).sortable({
      connectWith: ".radiator-controller",
      handle: ".drag-handle",
      stop: function(e) {
        if (!radiatorName) { return; }

        var sortedConfigs = [];

        var sortedIds = $('.radiator-controller > div').filter(function(i, elem) {
          return $(elem).attr('data-module').indexOf('graph') > -1;
        }).map(function(i, elem) {
          return $(elem).attr('id');
        });

        sortedIds.each(function(i, id) {
          sortedConfigs.push(configMap[id]);
        });

        store.customRadiators.storeConfiguration(radiatorName, sortedConfigs);
      }
    });
  }

  function isDragHandle(elem) {
    return elem.matches('.drag-handle') || elem.parentNode.matches('.drag-handle');
  }

  var isDraggingMouseDown = function(e) {
    if (isDragHandle(e.target)) { return; }

    e.stopPropagation();
    e.preventDefault();
    var selectionArea = timeRangeSelectionArea;
    $(window).mousemove(function(e) {
      if (!isDragging) {
        isDragging = true;
        dragStart = e.pageX;
      } else {
        selectionArea().show();
        selectionArea().width(Math.abs(dragStart - e.pageX));
        var axisTop = $(".axis svg").offset().top;
        selectionArea().offset({ top: axisTop, left: Math.min(dragStart, e.pageX) });
        detailsStart = cubismGraphs.xToTime(selectionArea().offset().left);
        detailsStop = cubismGraphs.xToTime(selectionArea().offset().left + selectionArea().width());
      }
    });
    $(window).mouseup(isDraggingMouseUp);
  };

  var isDraggingMouseUp = function(e) {
    e.stopPropagation();
    e.preventDefault();
    $(window).unbind("mousemove");
    $(window).unbind("mouseup");
    if (isDragging) {
      context.broadcast("time-range-updated", {start: detailsStart, stop: detailsStop});
      context.broadcast("time-range-selected");
      isTimeRangeSelected = true;
    } else if (isTimeRangeSelected) {
      timeRangeSelectionArea().hide();
      var stop = new Date().getTime();
      var start = parseInt(stop - (1000 * 60 * 60 * 3));
      context.broadcast("time-range-updated", {start: start, stop: stop});
      context.broadcast("time-range-deselected");
      isTimeRangeSelected = false;
    }
    isDragging = false;
  };

  function resolveModuleElement(moduleElement) {
    return document.getElementById($(moduleElement).attr("data-graph-module-id")); //FIXME uggglyyy
  }

  function removeGraphConfig(moduleElement) {
    var graphConfig = context.application.getModuleConfig(moduleElement);
    store.customRadiators.removeRadiatorConfig(radiatorName, graphConfig.chart);
  }

  return {
    init: function() {
      intercom   = context.getGlobal("Intercom").getInstance();
      d3         = context.getGlobal("d3");
      $          = context.getGlobal("jQuery");
      moduleElem = d3.select(context.getElement());

      utils          = context.getService("utils");
      windowSvc      = context.getService("window");
      store          = context.getService("configuration-store");
      cubismGraphs   = context.getService("cubism-graphs");
      metrics        = context.getService("metrics");

      $(window).resize(utils.debouncer(function() {
        moduleElem.html('');
        setTimeout(attachSorting, 500);
      }));

      attachSorting();

      host         = windowSvc.getHashVariable("host");
      radiatorName = windowSvc.getHashVariable("name");
      var configs      = host ? metrics.defaultMetrics(host) : store.customRadiators.readConfiguration(radiatorName);

      detailsStop  = parseInt(new Date().getTime());
      var timescale = windowSvc.getTimescale();
      detailsStart = parseInt(detailsStop - (1000 * timescale));
      var categoryUrl = '/metrics/categories?start=' + detailsStart + "&stop=" + detailsStop;

      if (host) {
        categoryUrl += "&tag=host_" + host;
      }
      // fetch category data prior to rendering graphs
      d3.json(categoryUrl, function(error, categories) {
        threaddumpChildren = categories.filter(function(category) {
          return category.indexOf('category_threaddump_') > -1;
        });

        configs.forEach(function(config, i) {
          // init all graphs found in radiator configuration
          configs[i] = config = config.chart ? config : { chart: config };
          initGraph(config);
          // wipe config if it is marked for deletion (e.g. single graph)
          if (config.removeAfterUse) {
            store.customRadiators.removeConfiguration(radiatorName);
          }
        });

        var metric = (configs[0].chart.metric || configs[0].chart.type).toUpperCase();
        if (configs.length === 1) {
          // we're showing single graph, might as well update title nicely
          windowSvc.setTitle(metric + " for " + configs[0].chart.host);
        } else {
          // update title with radiator name
          windowSvc.setTitle(radiatorName || host + " radiator");
        }
      });
    },

    onmousedown: isDraggingMouseDown,

    messages: ["timescale-changed"],

    onmessage: function(name, timescale) {
      switch (name) {
        case 'timescale-changed':
          cubismGraphs.resetCubismContext();
          break;
      }
    },

    onclick: function(event, element, elementType) {
      switch (elementType) {
        case 'close':
          var moduleElement = resolveModuleElement(element);
          removeGraphConfig(moduleElement);
          context.application.stop(moduleElement);
          break;
      }
    }
  };
});
