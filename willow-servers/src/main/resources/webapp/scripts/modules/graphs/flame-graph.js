Box.Application.addModule('flame-graph', function(context) {
  'use strict';

  var d3, host, windowSvc, metrics, store, utils, moduleElement, moduleConf,
      detailsStart, detailsStop, timescale, resetBtn, rect, text, x, y, moduleWidth, moduleHeight,
      TRANSITION_DURATION = 500;

  function yPos(d) {
    return moduleHeight - y(d.y) - y(d.dy);
  }

  function xPos(d) {
    return x(d.x);
  }

  function yPosClick(d) {
    return moduleHeight - y(d.y);
  }

  function barHeight(d) {
    return y(d.dy);
  }

  function barWidth(d) {
    return x(d.dx);
  }

  function textPosX(d) {
    return x(d.x) + 10;
  }

  function zoom(d) {
      x.domain([d.x, d.x + d.dx]);
      y.domain([d.y, 1]).range([d.y ? 20 : 0, moduleHeight]);

      rect.transition()
          .duration(TRANSITION_DURATION)
          .attr("x", xPos)
          .attr("y", yPosClick)
          // custom width and height when zooming
          .attr("width", function(d) { return x(d.x + d.dx) - x(d.x); })
          .attr("height", function(d) { return y(d.y + d.dy) - y(d.y); });

      text.transition()
          .each("start", function() {
            this.style.visibility = 'hidden';
          })
          .duration(TRANSITION_DURATION)
          .attr("x", textPosX)
          .attr("y", function(d) { return yPosClick(d) + y(d.y + d.dy) - y(d.y); })
          .attr("width", function(d) { return x(d.x + d.dx) - x(d.x); })
          .attr("height", function(d) { return y(d.y + d.dy) - y(d.y); })
          .transition().each("end", function (d) {
            var width = x(d.x + d.dx) - x(d.x) - 10;
            this.style.visibility = width > this.getComputedTextLength() ? '' : 'hidden';
          });
  }

  function createFlameGraph() {
    var colors = d3.scale.category20c(),
        partition = d3.layout.partition(),
        detailsStop = new Date().getTime(),
        detailsStart = parseInt(detailsStop - timescale),
        dataUrl = "/metrics/" + moduleConf.chart.metric +
                  '?start=' + detailsStart +
                  "&stop=" + detailsStop +
                  '&tag=host_' + host +
                  '&tag=' + moduleConf.chart.childtag;

    var graph = moduleElement.append("svg").classed('graph', true)
                .attr("width", moduleWidth)
                .attr("height", moduleHeight);

    resetBtn = graph.append("circle")
              .attr("cx", moduleWidth/2)
              .attr("cy", moduleHeight/2)
              .attr("r", "100%")//whole background
              .attr("fill", 'white')
              .on('click', reset);

    rect = graph.selectAll("rect");
    text = graph.selectAll("text");

    d3.json(dataUrl, function(error, root) {
      var nodes = partition.nodes(root);
      var gSelection = rect.data(nodes).enter().append("g")
            .style("cursor", "pointer")
            .on('mouseover', function(d){
              d3.select(this).select("text").style({visibility:'visible'});
            })
            .on('mouseout', function(d){
              if( d3.select(this).select("rect").node().attributes.width.value - 10 < d3.select(this).select("text").node().getComputedTextLength() ) {
                d3.select(this).select("text").style({visibility:'hidden'});
              }
            });

      rect = gSelection.append("rect")
            .attr("x", xPos)
            .attr("y", yPos)
            .attr("height", barHeight)
            .attr("width", barWidth)
            .attr("fill", function(d) { return colors((d.children ? d : d.parent).key); })
            .on("click", zoom);

      text = gSelection.append("text")
            .attr("class", "label")
            .attr("x", textPosX)
            .attr("y", function(d) { return yPos(d) + y(d.dy); })
            .text(function(d) { return d.key; })
            .on("click", zoom)
            .attr("style", function(d) {
              return (x(d.dx) - 10) > this.getComputedTextLength() ? '' : 'visibility:hidden';
            });
    });
  }

  function reset() {
    var style = window.getComputedStyle(moduleElement[0][0]);
    moduleWidth = parseInt(style.width) - parseInt(style.paddingLeft) - parseInt(style.paddingRight);
    moduleHeight = parseInt(style.height) - parseInt(style.paddingTop) - parseInt(style.paddingBottom);
    x = d3.scale.linear().range([0, moduleWidth]);
    y = d3.scale.linear().range([0, moduleHeight]);

    removeGraph();
    createFlameGraph();
  }

  function removeGraph() {
    moduleElement.selectAll(".graph").remove();
  }

  function openGraphInPopup() {
    var radiatorName = utils.guid(),
        url = '/radiator.html#name=' + radiatorName;

    moduleConf.removeAfterUse = true;
    store.customRadiators.appendConfiguration(radiatorName, moduleConf);
    delete moduleConf.removeAfterUse;

    windowSvc.popup({
      url: url,
      height: window.innerHeight * 0.75,
      width: window.innerWidth * 0.75
    });
  }

  return {
    init: function() {
      d3        = context.getGlobal("d3");
      windowSvc = context.getService("window");
      store     = context.getService("configuration-store");
      utils     = context.getService("utils");

      moduleElement = d3.select(context.getElement());
      moduleConf = context.getConfig() || {};
      moduleConf.chart = moduleConf.chart || {
        type: 'flame',
        metric: 'stacktrace',
        host: windowSvc.getHashVariable("host")
      };
      host = moduleConf.chart.host;
      timescale = windowSvc.getTimescale();

      var graphIconsElem = moduleElement.append("div").classed("nv-graph__icons", true);
      var extraLinkInfo = '(' + moduleConf.chart.childtag.split('category_threaddump_')[1] + ')';

      graphIconsElem.call(utils.appendHostRadiatorLink, moduleConf.chart.metric, host, extraLinkInfo);
      graphIconsElem.call(utils.appendShareRadiatorIcon, host);
      graphIconsElem.call(utils.appendPopupGraphIcon, host);
      graphIconsElem.call(utils.appendRemovalButton, moduleElement.attr('id'));
      graphIconsElem.call(utils.appendDraggableHandleIcon);

      reset();
    },

    destroy: function() {
      moduleElement.remove();
      moduleElement = null;
    },

    messages: [ "timescale-changed" ],

    onmessage: function(name, data) {
      switch (name) {
        case 'timescale-changed':
          timescale = data;
          detailsStop = new Date().getTime();
          detailsStart = detailsStop - timescale;
          reset();
          break;
      }
    },

    onclick: function(event, element, elementType) {
      switch (elementType) {
        case 'to-popup':
          openGraphInPopup();
          break;
        case 'to-radiator':
          context.broadcast("open-radiator-list", moduleConf.chart);
          break;
      }
    }

  };
});
