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
      var host = element.getAttribute("data-host");
      switch (elementType) {
      case 'start-terminal':
        windowSvc.open("shell.html?user=pasi&host=" + host, "_blank");
        break;
      case 'to-radiator':
        windowSvc.open("radiator.html?host=" + host, "radiator-" + host);
        break;
      case 'close':
        break;
      }
    },
    onmessage: function(name, data) {
      switch (name) {
      case 'timescale-changed':
        this.setTimescale(data);
        break;
      case 'metric-changed':
        this.setMetric(data);
        break;
      }
    },
    setTimescale: function(scale) {

    },
    setMetric: function(mtric) {
      metric = mtric;
      resetGraphs();
      shuffleNavigation();
    }
  };
}); 