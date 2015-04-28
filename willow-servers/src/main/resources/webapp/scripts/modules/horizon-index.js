Box.Application.addModule('horizon-index', function(context) {
  'use strict';
  var element;
  var windowSvc;

  return {
    init: function() {
      element = context.getElement();
      windowSvc = context.getService("window-service");
    },
    destroy: function() {
      element = null;
    },
    messages: ["timescale-changed", "metric-changed"],
    onclick: function(event, element, elementType) {
      switch (elementType) {
        case 'start-terminal':
          windowSvc.open("shell.html?user=pasi&host=" + element.getAttribute("data-host"), "_blank");
          break;
        case 'to-radiator':
          windowSvc.open("radiator.html?host=" + element.getAttribute("data-host"), "index-alerts");
          break;
        case 'close':
          break;
        };
      },
      setTimeScale: function(scale) {

      },
      setMetric: function(metric) {

      }
	};
}); 