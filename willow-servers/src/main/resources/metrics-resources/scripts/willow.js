var step = 3e4;
var size = Math.max(document.documentElement.clientWidth, window.innerWidth || 0) - 100;

var context = cubism.context()
				.step(step)
				.size(size)
				.start();

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

var initGraphs = function () {
	var stop = new Date().getTime();
	var start = stop - (step * size);
	d3.json("/metrics/hosts"
			+ "?start=" + start
			+ "&stop=" + stop 
			+ "&type=cpu", function(data) {
				if (!data) return new Error("unable to load data");
				for (var i=0; i<data.length; i++) {
					if ( ! $(".horizon-" + data[i]).length ) {
						var next = deployer_metric("cpu", data[i]);
						d3.select("#chart").call(function(div) {
							div.selectAll(".horizon-" + data[i])
							.data([next])
							.enter().append("div")
							.attr("class", "horizon horizon-" + data[i])
							.call(context.horizon()
									.height(50)
									.extent([0,100])
									.format(d3.format(".2f"))
									.title("Cpu for " + data[i]));

							div.append("div")
							.attr("class", "rule")
							.call(context.rule());
						});
					}
				}
			});
};

var resetGraphs = function () {
	d3.selectAll(".horizon").call(context.horizon().remove).remove(); 
	d3.selectAll(".axis").remove();
	d3.selectAll(".rule").remove();
	d3.select("#chart").attr("style", "width: " + size + "px");
	d3.select("#chart").call(function(div) {
		div.append("div")
		.attr("class", "axis")
		.call(context.axis().orient("top"));
	});
	initGraphs();
	context.on("focus", function(i) {
		format = d3.format(".2f");
		d3.selectAll(".horizon .value").style("right", i === null ? null : context.size() - i + "px");
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