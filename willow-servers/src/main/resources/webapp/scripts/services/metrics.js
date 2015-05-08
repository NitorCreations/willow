Box.Application.addService('metrics', function(application) {
  'use strict';

  function d3() {
    return application.getGlobal('d3');
  }

  function hostsUrl(metric, start, stop, step) {
    return "metrics/hosts" + "?start=" + start + "&stop=" + stop + "&type=" + metric;
  }
/*
  function metricUrl(type, instanceTag, start, stop, step) {
    return "metrics/" + type +
        "?start=" + start +
        "&stop=" + stop +
        "&step=" + step +
        "&tag=" + instanceTag;
  }*/

  return {
    hostsDataSource: function(metric, start, stop, step) {
      var dataUrl = hostsUrl(metric, start, stop, step);
      return function(callback) {
        d3().json(dataUrl, function(error, data) {
          if (!data) {
            return new Error("failed to load data from " + dataUrl + "reason:" + error);
          }
          callback(data);
        });
      };
    }
  };

  /*function metricsChart(_type, _instanceTag, _stop, _step) {
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
  }*/
});