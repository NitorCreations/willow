Box.Application.addModule('radiator-details', function(context) {
  'use strict';
  var nv, d3, $, host, windowSvc, moduleEl;
  var charts = [], detailsStop = -1, detailsStart = -1;
  
  var setupDetailsDivs = function(element, prefix, host) {
    var row = 1;
    if ($(element).children('.row').length === 0) {
      $(element).append('<div class="row row-' + host + ' row' + row + '-' + host + '">');
    } else {
      row =  $(element).children('.row').length;
      if ($(element).children('.row' + row + '-' + host).children('.col').length == 2) {
        row++;
        $(element).append('<div class="row row-' + host + ' row' + row + '-' + host + '">');
      }
    }
    $(".row" + row + "-" + host).append('<div class="' + prefix + '-' + host + ' col c6" style="height:200px">');
    $("." + prefix + "-" + host).append("<svg>");
  };

  var kiloBytesToString = function (kbytes) {
    var fmt = d3.format('.0f');
    if (kbytes < 1024) {
      return fmt(kbytes) + 'kB';
    } else if (kbytes < 1024 * 1024) {
      return fmt(kbytes / 1024) + 'MB';
    } else if (kbytes < 1024 * 1024 * 1024) {
      return fmt(kbytes / 1024 / 1024) + 'GB';
    } else {
      return fmt(kbytes / 1024 / 1024 / 1024) + 'TB';
    }
  };

  var heapGraphCallback = function(host) {
    return function(data) {
      var divHost = host;
      nv.addGraph(function() {
        var chart = nv.models.lineChart();
        chart.xAxis
          .tickFormat(function(d) { return d3.time.format('%X')(new Date(d)); });
        chart.yAxis
          .tickFormat(d3.format(',.2s'));
        d3.select('.heap-'  + divHost + ' svg')
          .datum(data)
          .transition().duration(500)
          .call(chart);
        charts["heap-" + host] = chart;
        $(window).resize(chart.update);
        return chart;
      });
    };
  };

  var accessGraphCallback = function(host) {
    return function(data) {
      var divHost = host;
      nv.addGraph(function() {
        var chart =  nv.models.multiBarChart()
          .margin({top: 30, right: 20, bottom: 50, left: 75})
          .showControls(false)
          .stacked(true);

        chart.xAxis
          .tickFormat(function(d) { return d3.time.format('%X')(new Date(d)); });
        chart.yAxis
          .tickFormat(d3.format('0f'));
        d3.select('.access-'  + divHost + ' svg')
          .datum(data)
          .transition().duration(500)
          .call(chart);
        charts["access-" + host] = chart;
        $(window).resize(chart.update);
        return chart;
      });
    };
  };

  var fsGraphCallback = function(host) {
    return function(data) {
      var divHost = host;
      nv.addGraph(function() {
        var chart = nv.models.multiBarHorizontalChart()
          .margin({top: 30, right: 20, bottom: 50, left: 75})
          .tooltips(false)
          .showControls(false)
          .stacked(true);
        chart.yAxis
          .tickFormat(kiloBytesToString);
        d3.select('.fs-'  + divHost + ' svg')
          .datum(data)
          .call(chart);
        charts["fs-" + host] = chart;
        $(window).resize(chart.update);
        return chart;
      });
    };
  };

  return {
    init: function() {
      $          = context.getGlobal("jQuery");
      d3         = context.getGlobal("d3");
      nv         = context.getGlobal("nv");
      windowSvc  = context.getService("window");
      moduleEl   = context.getElement();
      host       = windowSvc.getHashVariable("host");

      $(".shape-navterminal").attr("data-host", host);

      var host_stop = parseInt(detailsStop);
      if (host_stop < 0) {
        host_stop = parseInt(new Date().getTime());
      }
      var host_start = parseInt(detailsStart);
      if (host_start < 0) {
        host_start = parseInt(host_stop - (1000 * 60 * 60 * 3));
      }

      setupDetailsDivs(moduleEl, "fs", host);
      setupDetailsDivs(moduleEl, "heap", host);
      setupDetailsDivs(moduleEl, "access", host);

      d3.json("metrics/disk?tag=host_" + host + "&stop=" + host_stop, fsGraphCallback(host));
      d3.json("metrics/heap?tag=host_" + host + "&step=15000&start=" + host_start + "&stop=" + host_stop, heapGraphCallback(host));
      d3.json("metrics/access?tag=host_" + host + "&step=60000&start=" + host_start + "&stop=" + host_stop, accessGraphCallback(host));
    },

    messages: [ "details-updated" ],

    onmessage: function(name, data) {
      switch (name) {
      case 'details-updated':
        var chartId;
        for (chartId in charts) {
          if (chartId.startsWith(data) && charts.hasOwnProperty(chartId)) {
            charts[chartId].update();
          }
        }
        break;
      }
    }
  };
});
