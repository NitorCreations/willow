Box.Application.addModule('horizon-ruler', function(context) {
  'use strict';

  var d3, moduleElem, cubismGraphs;

  function initLayout() {
    moduleElem.select(".axis")
      .call(cubismGraphs
        .createGraphAxis()
        .orient("top")
        .tickFormat(d3.time.format("%H:%M"))
    );
    moduleElem.select(".rule")
      .call(cubismGraphs.createRulerOverGraphs());
  }

  function removeRulers() {
    moduleElem.selectAll('.axis > svg, .rule > svg').remove();
  }

  function reset() {
    removeRulers();
    initLayout();
  }

  return {
    init: function() {
      d3           = context.getGlobal("d3");
      moduleElem   = d3.select(context.getElement());
      cubismGraphs = context.getService("cubism-graphs");
      initLayout();
    },

    destroy: function() {
      removeRulers();
    },

    messages: ["cubism-context-reset"],

    onmessage: function(name, data) {
      switch (name) {
        case 'cubism-context-reset':
          reset();
          break;
      }
    }
  };
});
