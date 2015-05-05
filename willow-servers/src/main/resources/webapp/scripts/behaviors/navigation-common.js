Box.Application.addBehavior('navigation-common', function(context) {
  'use strict';
  var windowSvc, utils, $, moduleEl;
  var timescaleInHash = function(timescale) {
    utils.variableStateInHash("timescale", timescale, function(hash) {
      $("a", moduleEl).each(function(index, element) {
        $(element).attr("href", "#" + utils.addOrReplaceUrlVariable(hash, "metric", element.getAttribute("data-metric")));
      });
    });
  };

  return {
    init: function() {
      windowSvc = context.getService("window");
      utils = context.getService("utils");
      $ = context.getGlobal("jQuery");
      moduleEl = context.getElement();
      var timescale = utils.getHashVariable("timescale") || 10800;
      timescaleInHash(timescale);
    },
    destroy: function() {
      windowSvc = null;
    },
    onclick: function(event, element, elementType) {
      switch (elementType) {
        case 'alerts':
          windowSvc.openAlerts();
          break;
      }
    },
    onchange: function(event, element, elementType) {
      switch (elementType) {
        case 'select-timescale':
          var timescale = element.children[element.selectedIndex].getAttribute("value");
          timescaleInHash(timescale);
          context.broadcast("timescale-changed", timescale);
          break;
      }
    }
  };
});