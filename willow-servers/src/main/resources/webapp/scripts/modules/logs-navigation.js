Box.Application.addModule('logs-navigation', function(context) {
  'use strict';
  var socket, seen  = [], d3, timeFormatter;
  var pollAlerts = function() {
    var pollConf = {};
    pollConf.stop = new Date().getTime();
    pollConf.start = pollConf.stop - (1000 * 60 * 60 * 12);
    pollConf.step = 10000;
    pollConf.minSteps = 30;
    pollConf.metricKey = "/log";
    var conf = JSON.stringify(pollConf);
    socket.send(conf);
  };
  var onEvents = function(event) {
    var willowEvents = JSON.parse(event.data);
    var i;
    for (i=0; i<willowEvents.data.length; i++) {
      if (!seenEvent(willowEvents.data[i].id)) {
        $("tbody").prepend("<tr><td>" + timeFormatter(new Date(willowEvents.data[i].timestamp)) + "</td>" + 
        "<td>" +  willowEvents.data[i].logger + "</td>" + 
        "<td>" +  willowEvents.data[i].level + "</td>" + 
        "<td>" + willowEvents.data[i].message + "</td></tr>");
      }
    }
  };
  var seenEvent = function(id) {
    if (seen.indexOf(id) > -1) {
      return true;
    } else {
      seen.push(id);
      return false;
    }
  };
  return {
    init: function() {
      d3 = context.getGlobal("d3");
      timeFormatter = d3.time.format("%Y-%m-%d %X");
      var loc = window.location, ws_uri;
      if (loc.protocol === "https:") {
        ws_uri = "wss:";
      } else {
        ws_uri = "ws:";
      }
      var ctx = "/";
      var ctxEnd = loc.pathname.lastIndexOf("/");
      if (ctxEnd > 0) {
        if (loc.pathname.indexOf("/") === 0) {
          ctx = "";
        }
        ctx += loc.pathname.substring(0, contextEnd) + "/";
      }
      ws_uri += "//" + loc.host + ctx + "poll/";
      socket = new WebSocket(ws_uri);
      socket.onopen = pollAlerts;
      socket.onmessage = onEvents;
    }
  };
});