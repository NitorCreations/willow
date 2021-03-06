Box.Application.addService('metrics', function(application) {
  'use strict';

  var d3 = application.getGlobal('d3');
  var utils = application.getService("utils");

  var promiseCache = {};

  function createDataSource(url) {
    return function(callback) {
      // attach callback to existing promise if there is one available
      var promise = promiseCache[url] || d3.promise.json(url);

      promise.then(function (data) {
        callback(data);
        promiseCache[url] = null;
      }, function(err) {
        console.error("failed to load data from " + url + "reason:" + err);
      });

      promiseCache[url] = promise;
    };
  }

  /**
   * Fetch hosts with specific metric between specific time range
   */
  function hostsUrl(metric, start, stop) {
    return "metrics/hosts" + "?start=" + start + "&stop=" + stop + "&type=" + metric;
  }

  /**
   * Fetch metrics for specific instance between certain interval
   */
  function metricUrl(type, instanceTag, start, stop, step) {
    return "metrics/" + type +
        "?start=" + start +
        "&stop=" + stop +
        "&step=" + step +
        "&tag=" + instanceTag;
  }

  function defaultMetrics(host) {
    var defaults = ["cpu", "mem", "net", "diskio", "tcpinfo"];
    var configs = defaults.map(function(metric) {
        return {
          metric: metric,
          host: host,
          instanceTag: "host_" + host,
          type: "horizon"
        };
      }
    );
    configs.push({ type: 'filesystem', host: host });
    configs.push({ type: 'heap', host: host });
    configs.push({ type: 'flame', host: host, metric: 'stacktrace' });
    configs.push({ type: 'access', host: host });
    configs.push({ type: 'childcpu', host: host });
    return configs;
  }

  return {
    hostsDataSource: function(metric, start, stop) {
      var dataUrl = hostsUrl(metric, start, stop);
      return createDataSource(dataUrl);
    },
    metricsDataSource: function(type, instanceTag, start, stop, step) {
      var dataUrl = metricUrl(type, instanceTag, start, stop, step);
      return createDataSource(dataUrl);
    },
    defaultMetrics: defaultMetrics
  };
});