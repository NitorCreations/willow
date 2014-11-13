var step = 3e4;
var size;
var isDragging = false;
var dragStart = 0;
var detailsStart = -1;
var detailsStop = -1;
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
var defaultColors = ["#08519c", "#3182bd", "#6baed6", "#bdd7e7", "#bae4b3", "#74c476", "#31a354", "#006d2c"];
var metricMap = {
		"cpu" : { "title" : "cpu: ", "format" : d3.format(".2f"), "extent": [0, 100], colors : defaultColors },
		"mem" : { "title" : "mem: ", "format" : d3.format(".2f"), "extent": [0, 100], colors : defaultColors },
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
var updateCharts = function() {
	var chartId;
	for (chartId in charts) {
		if (charts.hasOwnProperty(chartId)) {
			charts[chartId].update();
		}
	}
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
  		$(element).append('<div class="row row-' + host + '">');
  		$(".row-" + host).append('<div class="fs-' + host + ' col c6" style="height:300px">')
	  	$(".fs-" + host).append("<svg>");
  		$(".row-" + host).append('<div class="heap-' + host + ' col c6" style="height:300px">')
	  	$(".heap-" + host).append("<svg>");
  		var host_stop = detailsStop;
  		if (host_stop < 0) {
	      host_stop = new Date().getTime();
  		}
	    
	    d3.json("/metrics/disk?tag=host_" + host + "&stop=" + host_stop, function(data) {
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
	    });
	    var host_start = detailsStart;
	    if (host_start < 0) {
	      host_start = host_stop - (1000 * 60 * 60 * 3);
	    }
	    d3.json("/metrics/heap?tag=host_" + host + "&step=15000&start=" + host_start + "&stop=" + host_stop, function(data) {
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
	    });
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
var isDraggingMouseUp = function(e) {
	$(window).unbind("mousemove");
	$(window).unbind("mouseup");
	var host = $(mouseDownTarget).attr("data-host");
  	var element = ".details-" + host;
	if (!isDragging || ! $(element).attr("data-expanded")) {
		expandDetails(e);
		if (!isDragging) {
			$(".selection").hide();
		}
	} else if (isDragging) {
	    d3.json("/metrics/disk?tag=host_" + host + "&stop=" + detailsStop, function(data) {
 	        d3.select('.fs-'  + host + ' svg').datum(data);
	 		updateCharts();
        });
	    d3.json("/metrics/heap?tag=host_" + host + "&step=15000&start=" + detailsStart + "&stop=" + detailsStop, function(data) {
	 	    d3.select('.heap-'  + host + ' svg').datum(data);
	 		updateCharts();
	    });
	}
	isDragging = false;
	e.stopPropagation();
};
var initGraphs = function () {
	var stop = new Date().getTime();
	var start = stop - (step * size);
	d3.json("/metrics/hosts"
			+ "?start=" + start
			+ "&stop=" + stop 
			+ "&type=" + metric, function(data) {
				$(".horizon").unbind("mousedown");
				data.sort();
				if (!data) return new Error("unable to load data");
				for (var i=0; i<data.length; i++) {
					var host = data[i].substring(5);
					if ( ! $(".horizon-" + host).length ) {
						var metricSettings = $(metricMap).attr(metric);
						var next = deployer_metric(metric, data[i]);
						d3.select("#chart").call(function(div) {
							div.selectAll(".horizon-" + host)
							.data([next])
							.enter().append("div")
							.attr("class", "horizon horizon-" + host + " horizoncpu-" + host)
							.attr("data-host", host)
							.call(context.horizon()
									.height(50)
									.colors(metricSettings.colors)
									.extent(metricSettings.extent)
									.format(metricSettings.format)
									.title(metricSettings.title + host));
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
	$("#" + metric).prependTo(".nav .container");
}
var refresh = window.setInterval(initGraphs, 3000);
$(window).resize(debouncer(function (e) {
	    window.clearInterval(refresh);
		resetGraphs(); 
		refresh = window.setInterval(initGraphs, 3000);
}));
