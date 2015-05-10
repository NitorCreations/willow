Box.Application.addModule('horizon-index', function(context) {
  'use strict';

  var moduleElem, windowSvc, d3, cubismGraphs, utils, timescale;

  function initLayout(widthInPixels) {
    moduleElem.attr("style", "width: " + widthInPixels + "px");

    moduleElem.insert("div", ":first-child")
      .classed("axis", true)
      .call(cubismGraphs.createGraphAxis().orient("top").tickFormat(d3.time.format("%H:%M")));

    moduleElem.insert("div", ":first-child")
      .classed("rule", true)
      .call(cubismGraphs.createRulerOverGraphs());
  }

  function reset() {
    var step = parseInt(timescale * 1000 / window.innerWidth);

    moduleElem.selectAll('.axis, .rule').remove();

    cubismGraphs.resetCubismContext(step, window.innerWidth);
    initLayout(window.innerWidth);
  }

  return {
    init: function() {
      d3           = context.getGlobal("d3");
      moduleElem   = d3.select(context.getElement());

      utils        = context.getService("utils");
      windowSvc    = context.getService("window");
      cubismGraphs = context.getService("cubism-graphs");

      timescale    = windowSvc.getHashVariable("timescale") || 10800; // TODO: configSvc for configs

      $(window).resize(utils.debouncer(reset));
      reset();
    },

    destroy: function() {
    },

    onclick: function(event, element, elementType) {
    },

    onmessage: function(name, data) {
    }
  };
});
