var step = 3e4;
var size;
var isDragging = false;
var dragStart = 0;
var detailsStart = -1;
var detailsStop = -1;
if (typeof String.prototype.startsWith != 'function') {
      String.prototype.startsWith = function (str){
        return this.indexOf(str) == 0;
      };
}
var moveSelection = function() {
    if ($(".selection").is(":visible")) {
        var nowLeft = $(".selection").offset().left;
        if (nowLeft > 0) {
            $(".selection").offset({ top: 50, left: nowLeft - 1 });
        } else {
            var nowWidth = $(".selection").width();
            if (nowWidth > 1) {
                $(".selection").width(nowWidth - 1);
            }
        }
    }
}
var context = cubism.context()
                .step(step)
                .size(size)
                .on("change", moveSelection)
                .start();
var mouseDownTarget = null;
var charts = {};
var hosts = [];
var intervals = {};
var defaultColors = ["#08519c", "#3182bd", "#6baed6", "#bdd7e7", "#bae4b3", "#74c476", "#31a354", "#006d2c"];
var cpuColors = ["#08519c", "#3182bd", "#6baed6", "#bdd7e7", "#bae4b3", "#006d2c", "#b07635", "#d01717"];
var metricMap = {
        "cpu" : { "title" : "cpu: ", "format" : d3.format(".2f"), "extent": [0, 100], colors : cpuColors },
        "mem" : { "title" : "mem: ", "format" : d3.format(".2f"), "extent": [0, 100], colors : cpuColors },
        "net" : { "title" : "net: ", "format" : d3.format(".2f"), "extent": undefined, colors : defaultColors },
        "diskio" : { "title" : "io: ", "format" : d3.format(".2f"), "extent": undefined, colors : defaultColors },
        "tcpinfo" : { "title" : "conn: ", "format" : d3.format(".0f"), "extent": undefined, colors : defaultColors }
}
var getQueryVariable = function(variable) {
       var query = window.location.search.substring(1);
       var vars = query.split("&");
       for (var i=0;i<vars.length;i++) {
               var pair = vars[i].split("=");
               if(pair[0] == variable){return pair[1];}
       }
       return(false);
}
var metric = getQueryVariable("metric") ? getQueryVariable("metric") : "cpu"; 
var xToTime = function(pageX) {
    var timeStart = context.scale.domain()[0].getTime();
    return timeStart + (pageX * step);
}
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
}
var deployer_metric = function(name, tag) {
    var hostTag = tag;
    return context.metric(function(start, stop, step, callback) {
        d3.json("/metrics/" + name 
                + "?start=" + start.getTime()
                + "&stop=" + stop.getTime()
                + "&step=" + step + "&tag=" + hostTag, function(data) {
            if (!data) return callback(new Error("unable to load data"));
            callback(null, data.map(function(d) { return d.value; }));
        });
    }, name += "");
};
var updateCharts = function(prefix) {
    var chartId;
    for (chartId in charts) {
        if (chartId.startsWith(prefix) && charts.hasOwnProperty(chartId)) {
            charts[chartId].update();
        }
    }
};
var setupDetailsDivs = function(element, prefix, host) {
    var row = 1;
    if ($(element).children('.row').length == 0) {
          $(element).append('<div class="row row-' + host + ' row' + row + '-' + host + '">');
    } else {
        row =  $(element).children('.row').length;
        if ($(element).children('.row' + row + '-' + host).children('.col').length == 2) {
            row++;
            $(element).append('<div class="row row-' + host + ' row' + row + '-' + host + '">');
        }
    }
    $(".row" + row + "-" + host).append('<div class="' + prefix + '-' + host + ' col c6" style="height:200px">')
    $("." + prefix + "-" + host).append("<svg>");
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
var heapGraphCallback = function(host) {
    return function(data) {
        var divHost = host;
        nv.addGraph(function() {
          var chart = nv.models.lineChart();
          chart.xAxis
              .tickFormat(function(d) {
                  return d3.time.format('%X')(new Date(d))
                });
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
              .tickFormat(function(d) {
                  return d3.time.format('%X')(new Date(d))
                });
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
var expandDetails = function(e) {
    var host = $(mouseDownTarget).attr("data-host");
      var element = ".details-" + host;
      if ($(element).attr("data-expanded")) {
          $(element).slideUp("slow", function() {
              d3.selectAll(".row-" + host + " div svg").remove();
              d3.selectAll(".row-" + host + " div").remove();
              d3.selectAll(".row-" + host).remove();
              $(window).unbind("resize", charts["fs-" + host].update);
              $(window).unbind("resize", charts["heap-" + host].update);
              delete charts["fs-" + host];
              delete charts["heap-" + host];
              $(this).removeAttr("data-expanded");
          });
      } else {
          var host_stop = parseInt(detailsStop);
          if (host_stop < 0) {
          host_stop = parseInt(new Date().getTime());
          }
        var host_start = parseInt(detailsStart);
        if (host_start < 0) {
          host_start = parseInt(host_stop - (1000 * 60 * 60 * 3));
        }
        setupDetailsDivs(element, "fs", host);
        setupDetailsDivs(element, "heap", host);
        setupDetailsDivs(element, "access", host);
        $(".row2-" + host).append('<div class="login-' + host + ' col c6" style="height:200px">')
        $(".login-" + host).append('<a class="btn btn-b smooth login">Login</a>');
        $(".login-" + host).on("click", function() {
              window.open("/shell/?user=pasi&host=localhost");
        });
        d3.json("/metrics/disk?tag=host_" + host + "&stop=" + host_stop, fsGraphCallback(host));
        d3.json("/metrics/heap?tag=host_" + host + "&step=15000&start=" + host_start + "&stop=" + host_stop, heapGraphCallback(host));
        d3.json("/metrics/access?tag=host_" + host + "&step=60000&start=" + host_start + "&stop=" + host_stop, accessGraphCallback(host));
        $(element).slideDown("slow", function() {
            $(this).attr("data-expanded", "true");
        });
    }
    e.stopPropagation();
};
var isDraggingMouseDown = function(e) {
    mouseDownTarget = e.currentTarget;
    $(window).mousemove(function(e) {
        if (!isDragging) {
          isDragging = true;
          dragStart = e.pageX;
        } else {
          $(".selection").show();
          $(".selection").width(Math.abs(dragStart - e.pageX));
          $(".selection").offset({ top: 50, left: Math.min(dragStart, e.pageX) });
          detailsStart = xToTime($(".selection").offset().left);
          detailsStop = xToTime($(".selection").offset().left + $(".selection").width());
        }
    });
    $(window).mouseup(isDraggingMouseUp);
    e.stopPropagation();
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
    var host = $(mouseDownTarget).attr("data-host");
      var element = ".details-" + host;
    if (!isDragging || ! $(element).attr("data-expanded")) {
        if (!isDragging) {
            $(".selection").hide();
            detailsStart = -1;
            detailsStop = -1;
        }
        expandDetails(e);
    } 
    if (isDragging) {
        for (var i=0; i<hosts.length; i++) {
            var nextHost = hosts[i];
              var element = ".details-" + nextHost;
              if ($(element).attr("data-expanded")) {
                  d3.json("/metrics/disk?tag=host_" + nextHost+ "&stop=" + detailsStop, 
                          updateChart(nextHost, "fs-"));
                  d3.json("/metrics/heap?tag=host_" + nextHost + "&step=15000&start=" + detailsStart + "&stop=" + detailsStop, 
                          updateChart(nextHost, "heap-"));
                  d3.json("/metrics/access?tag=host_" + nextHost + "&step=15000&start=" + detailsStart + "&stop=" + detailsStop, 
                          updateChart(nextHost, "access-"));
              }
        }
    }
    isDragging = false;
    e.stopPropagation();
};
var initGraphs = function () {
    var stop = new Date().getTime();
    var start = stop - (step * size);
    while(hosts.length > 0) {
        hosts.pop();
    }
    d3.json("/metrics/hosts"
            + "?start=" + start
            + "&stop=" + stop 
            + "&type=" + metric, function(data) {
                $(".horizon").unbind("mousedown");
                data.sort();
                if (!data) return new Error("unable to load data");
                for (var i=0; i<data.length; i++) {
                    var host = data[i].substring(5);
                    hosts.push(host);
                    if ( ! $(".horizon-" + host).length ) {
                        var metricSettings = $(metricMap).attr(metric);
                        var next = deployer_metric(metric, data[i]);
                        d3.select("#chart").call(function(div) {
                            var graphDiv = div.selectAll(".horizon-" + host)
                            .data([next])
                            .enter().append("div");
                            
                            graphDiv.attr("class", "horizon horizon-" + host + " horizoncpu-" + host)
                            .attr("data-host", host)
                            .call(context.horizon()
                                    .height(50)
                                    .colors(metricSettings.colors)
                                    .extent(metricSettings.extent)
                                    .format(metricSettings.format)
                                    .title(metricSettings.title + host));
                            graphDiv.append("svg").attr("viewBox", "0 0 124 124")
                            	.attr("class", "icon shape-terminal terminal-" + host)
                            	.append("use").attr("xlink:href","#shape-terminal");
                            graphDiv.append("svg").attr("viewBox", "0 0 100 100")
                          	    .attr("class", "icon shape-share share-" + host)
                        	    .append("use").attr("xlink:href","#shape-to-radiator");
                            div.append("div")
                                .attr("class", "details details-" + host);
                        });
                    }
                }
                $(".horizon").mousedown(isDraggingMouseDown);
            });
};
var resetGraphs = function () {
    size = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
    start = stop - (step * size);
    context.stop();
    context = cubism.context()
        .step(step)
        .size(size)
        .on("change", moveSelection)
        .start();
    $(".horizon").unbind("mousedown");
    d3.selectAll(".horizon").call(context.horizon().remove).remove(); 
    d3.selectAll(".details").remove();
    d3.selectAll(".axis").remove();
    d3.selectAll(".rule").remove();
    d3.select("#chart").attr("style", "width: " + size + "px");
    d3.select("#chart").call(function(div) {
        div.append("div")
        .attr("class", "axis")
        .call(context.axis()
                .orient("top")
                .tickFormat(d3.time.format("%H:%M")));
    });
    initGraphs();
    context.on("focus", function(i) {
        d3.selectAll(".horizon .value").style("right", i === null ? null : context.size() - i + "px");
    });
    d3.select("#chart").call(function(div) {
        div.append("div")
        .attr("class", "rule")
        .call(context.rule());
    });
    d3.select("#chart").call(function(div) {
        div.append("div")
        .attr("class", "selection");
    });
};
var debouncer = function(func , timeout) {
    var timeoutID , timeout = timeout || 200;
    return function () {
        var scope = this , args = arguments;
        clearTimeout( timeoutID );
        timeoutID = setTimeout( function () {
            func.apply( scope , Array.prototype.slice.call( args ) );
        } , timeout );
    }
};
var shuffleNavigation = function() {
	$(".nav .container a").attr("class", "");
	$("#" + metric).attr("class", "pagename current");
	if ($(window).width() < 500) {
		$("#" + metric).prependTo(".nav .container");
	}
}
var refresh = window.setInterval(initGraphs, 3000);
$(window).resize(debouncer(function (e) {
        window.clearInterval(refresh);
        resetGraphs(); 
        refresh = window.setInterval(initGraphs, 3000);
}));