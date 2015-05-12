Box.Application.addModule('radiator-index', function(context) {
    'use strict';

    var d3, moduleElem, radiatorName, host, instanceTag, timescale,
        store, windowSvc, cubismGraphs, utils, metricsService;

    function initLayout(widthInPixels) {
        moduleElem.attr("style", "width: " + widthInPixels + "px");

        moduleElem.insert("div", ":first-child")
            .classed("axis", true)
            .call(cubismGraphs.createGraphAxis().orient("top").tickFormat(d3.time.format("%H:%M")));

        moduleElem.insert("div", ":first-child")
            .classed("rule", true)
            .call(cubismGraphs.createRulerOverGraphs());
    }

    function reset() {
        var widthInPx = $(window).width();
        var step = parseInt(timescale * 1000 / widthInPx);
        var stop = new Date().getTime();

        moduleElem.selectAll('.axis, .rule').remove();

        cubismGraphs.resetCubismContext(step, window.innerWidth);
        initLayout(window.innerWidth);
        initGraphs(stop, step);
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
        horizonGraphElement.html("<script type='text/x-config'>" + JSON.stringify(radiatorConfig) + "</script>");
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
        },

        destroy: function() {
        },

        onclick: function(event, element, elementType) {
        },

        onmessage: function(name, data) {
        }
    };
});
