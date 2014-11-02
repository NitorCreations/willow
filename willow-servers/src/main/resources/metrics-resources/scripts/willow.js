var step = 3e4;
var size = Math.max(document.documentElement.clientWidth, window.innerWidth || 0) - 100;

var context = cubism.context()
				.step(step)
				.size(size)
				.start();
var defaultColors = ["#08519c", "#3182bd", "#6baed6", "#bdd7e7", "#bae4b3", "#74c476", "#31a354", "#006d2c"];
var metricMap = {
		"cpu" : { "title" : "cpu: ", "format" : d3.format(".2f"), "extent": [0, 100], colors : defaultColors },
		"mem" : { "title" : "mem: ", "format" : d3.format(".2f"), "extent": [0, 100], colors : defaultColors },
		"net" : { "title" : "net: ", "format" : d3.format(".2fkB"), "extent": undefined, colors : defaultColors }
}

function getQueryVariable(variable) {
       var query = window.location.search.substring(1);
       var vars = query.split("&");
       for (var i=0;i<vars.length;i++) {
               var pair = vars[i].split("=");
               if(pair[0] == variable){return pair[1];}
       }
       return(false);
}

var metric = getQueryVariable("metric") ? getQueryVariable("metric") : "cpu"; 

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
var expandDetails = function() {
  	var element = ".details-" + $(this).attr("data-host");
  	if ($(element).attr("data-expanded")) {
  		$(element).slideUp("slow", function() {
  			$(this).removeAttr("data-expanded");
  		});
  	} else {
  		$(element).slideDown("slow", function() {
  			$(this).attr("data-expanded", "true");
  		});
    }
};
var initGraphs = function () {
	var stop = new Date().getTime();
	var start = stop - (step * size);
	d3.json("/metrics/hosts"
			+ "?start=" + start
			+ "&stop=" + stop 
			+ "&type=" + metric, function(data) {
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
    						    .attr("class", "details details-" + host).append("p").text("foo");
						});
					}
				}
				$(".horizon").click(expandDetails);
			});
};
var resetGraphs = function () {
	size = Math.max(document.documentElement.clientWidth, window.innerWidth || 0) - 100;
	start = stop - (step * size);
	context.stop();
	context = cubism.context()
		.step(step)
		.size(size)
		.start();
	$(".horizon").unbind("click");
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