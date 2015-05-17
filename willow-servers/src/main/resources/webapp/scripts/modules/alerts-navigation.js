Box.Application.addModule('alerts-navigation', function(context) {
  'use strict';
  var socket, seen  = [], d3, timeFormatter;
  var pollAlerts = function() {
    var pollConf = {};
    pollConf.stop = new Date().getTime();
    pollConf.start = pollConf.stop - (1000 * 60 * 60 * 12);
    pollConf.step = 10000;
    pollConf.minSteps = 30;
    pollConf.metricKey = "/event";
    var conf = JSON.stringify(pollConf);
    socket.send(conf);
  };
  var onEvents = function(event) {
    var willowEvents = JSON.parse(event.data);
    var i;
    for (i=0; i<willowEvents.length; i++) {
      if (!seenEvent(willowEvents[i].id)) {
        $("tbody").prepend("<tr><td>" + timeFormatter(new Date(willowEvents[i].timestamp)) + "</td><td>" +
          willowEvents[i].eventType.substring(willowEvents[i].eventType.lastIndexOf(".") + 1) +
          "</td><td>" + willowEvents[i].description + "</td></tr>");
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
      ws_uri += "//" + loc.host + "/poll/";
      socket = new WebSocket(ws_uri);
      socket.onopen = pollAlerts;
      socket.onmessage = onEvents;
    }
  };
});