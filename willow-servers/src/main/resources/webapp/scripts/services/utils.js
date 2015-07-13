Box.Application.addService('utils', function(application) {
  'use strict';
  var $ = application.getGlobal('jQuery');
  var d3 = application.getGlobal('d3');
  var socket;
  var messageHandlers = {};
  var pollConfs = {};
  var registeredConfs = [];

  var onmessage = function(event) {
    var eventData = JSON.parse(event.data);
    if (messageHandlers[eventData.id]) {
      messageHandlers[eventData.id](eventData.data);
    }
  };
  var registerPending = function() {
    if (socket && socket.readyState === 1) {
      for (var metricId in pollConfs) {
        if (registeredConfs.indexOf(metricId) === -1) {
          socket.send( JSON.stringify(pollConfs[metricId]) );
          registeredConfs.push(metricId);
        }
      }
    }
  };
  var mergePointArrays = function(chartPoints, parsedPoints) {
    if (!chartPoints || !parsedPoints || parsedPoints.length === 0) {
      return chartPoints;
    }
    var halfStep = (chartPoints[1].x - chartPoints[0].x) / 2;
    for (var i=0; i<parsedPoints.length; i++) {
      var nextPoint = parsedPoints[i];
      if (nextPoint.x >= chartPoints[chartPoints.length - 1].x + halfStep) {
        chartPoints.push(nextPoint);
        chartPoints.shift();
      } else {
        for (var j=chartPoints.length - 1; j>-1; j--) {
          if (nextPoint.x > chartPoints[j].x - halfStep &&
           nextPoint.x <= chartPoints[j].x + halfStep) {
             chartPoints[j].y = nextPoint.y;
             chartPoints[j].y0 = nextPoint.y;
             chartPoints[j].y1 = nextPoint.y;
             continue;
          }
        }
      }
    }
    return chartPoints;
  };
  return {
    debouncer: function(func , timeout) {
      var timeoutID , tmOut = timeout || 200;
      return function () {
        var scope = this , args = arguments;
        clearTimeout( timeoutID );
        timeoutID = setTimeout( function () {
          func.apply( scope , Array.prototype.slice.call( args ) );
        } , tmOut );
      };
    },
    // check if object contains a property
    contains: function(where, what) {
      for (var key in where) {
        return (key === what);
      }
    },
    setConfigurationElement: function(container, config) {
      container.html("<script type='text/x-config'>" + JSON.stringify(config) + "</script>");
    },
    /*
     * Replaces special characters with HTML entities
     *
     * @param {String} str
     * @returns {String} str with special chars replaced to HTML entities
     *
     */
    htmlEncode: function(str) {
      var el = document.createElement("div");
      el.innerText = el.textContent = str;
      str = el.innerHTML;
      return str;
    },

    // generates a new guid
    guid: function() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,
        function(c) {
          var r = Math.random() * 16 | 0,
            v = c == 'x' ? r : (r & 0x3 | 0x8);
          return v.toString(16);
        }).toUpperCase();
    },

    getStep: function(timescale, widthInPx) {
      widthInPx = widthInPx || $(window).width();
      return parseInt(timescale * 1000 / widthInPx);
    },

    appendPopupGraphIcon: function(parentElement, host) {
      return parentElement.append("svg")
        .attr("viewBox", "0 0 100 100")
        .classed("icon popup-" + host, true)
        .attr("data-type", "to-popup")
        .append("use").attr("xlink:href", "#shape-external-link");
    },

    appendDraggableHandleIcon: function(parentElement) {
    return parentElement.append("svg")
        .attr("viewBox", "0 0 100 100")
        .classed("icon drag-handle", true)
        .attr("data-type", "graph-drag")
        .append("use").attr("xlink:href", "#shape-move");
    },

    appendShareRadiatorIcon: function(parentElement, host) {
      return parentElement.append("svg")
        .attr("viewBox", "0 0 100 100")
        .classed("icon share-" + host, true)
        .attr("data-type", "to-radiator")
        .append("use").attr("xlink:href", "#shape-to-radiator");
    },

    appendTerminalIcon: function(parentElement, host) {
      return parentElement.append("svg")
        .attr("viewBox", "0 0 100 100")
        .classed("icon shape-terminal terminal-" + host, true)
        .attr("data-type", "start-terminal")
        .attr("data-host", host)
        .append("use").attr("xlink:href", "#shape-terminal");
    },

    appendRemovalButton: function(parentElement, parentModuleId) {
      return parentElement.append("svg")
        .attr("viewBox", "0 0 100 100")
        .classed("icon remove", true)
        .attr("data-type", "close")
        .attr("data-graph-module-id", parentModuleId)
        .append("use").attr("xlink:href", "#shape-close2");
    },

    appendHostRadiatorLink: function(parentElement, title, host, extraInfo) {
      extraInfo = extraInfo || "";
      var link = parentElement.append("div").classed("host-link", true);
      link.append("span").text(title);
      link.append("a")
        .attr("href", "radiator.html#host=" + host)
        .attr("data-host", host)
        .attr("data-type", "host-radiator").text(host);
      link.append("small").text(extraInfo);
      return link;
    },

    getDetailsSteps: function(moduleElement, timescale) {
      var steps = timescale;
      var detailsStep = 1;
      var i = 0;
      while (steps > ($(moduleElement[0]).width() / 2)) {
        detailsStep = scales[i].step;
        legend = scales[i].legend;
        steps = parseInt((detailsStop - detailsStart) / detailsStep);
        i++;
      }
    },

    configureSocket: function(opts, onmsg) {
      if (!socket) {
        var loc = window.location;
        var ctx = "/";
        var ctxEnd = loc.pathname.lastIndexOf("/");
        if (ctxEnd > 0) {
          if (loc.pathname.indexOf("/") === 0) {
            ctx = "";
          }
          ctx += loc.pathname.substring(0, contextEnd) + "/";
        }
        var ws_uri = (loc.protocol === 'https:' ? 'wss://' : 'ws://') + loc.host + ctx +"poll/";
        socket = new WebSocket(ws_uri);
        socket.onopen = registerPending;
      }
      var metricId = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
          var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
          return v.toString(16);
        });
      messageHandlers[metricId] = onmsg;
      var pollConf = {
        id: metricId,
        metricKey: "/" + opts.metricKey,
        start: opts.start,
        stop: opts.stop,
        step: opts.step,
        minSteps: 10
      };
      pollConfs[metricId] = pollConf;
      socket.onmessage = onmessage;
      registerPending();
      return socket;
    },
    mergePoints: function(chartData, parsedData) {
      parsedData.forEach(function(series) {
        for (var i=0; i<chartData.length; i++) {
          if (chartData[i].key === series.key) {
            chartData[i].values = mergePointArrays(chartData[i].values, series.values);
          }
        }
      });
    },
    timeFormat: d3.time.format("%H:%M"),
    dateFormat: d3.time.format("%a %e. %B"),
    dayTimeFormat: d3.time.format("%a %H:%M")
  };
});
