Box.Application.addService('metrics', function(application) {
  'use strict';

  function createDataSource(url) {
    var d3 = application.getGlobal('d3');
    return function(callback) {
      d3.json(url, function(error, data) {
        if (!data) {
          return new Error("failed to load data from " + url + "reason:" + error);
        }
        callback(data);
      });
    };
  }

  function hostsUrl(metric, start, stop, step) {
    return "metrics/hosts" + "?start=" + start + "&stop=" + stop + "&type=" + metric;
  }

  function metricUrl(type, instanceTag, start, stop, step) {
    return "metrics/" + type +
        "?start=" + start +
        "&stop=" + stop +
        "&step=" + step +
        "&tag=" + instanceTag;
  }

  return {
    hostsDataSource: function(metric, start, stop, step) {
      var dataUrl = hostsUrl(metric, start, stop, step);
      return createDataSource(dataUrl);
    },
    metricsDataSource: function(type, instanceTag, start, stop, step) {
      var dataUrl = metricUrl(type, instanceTag, start, stop, step);
      return createDataSource(dataUrl);
    }
  };
});