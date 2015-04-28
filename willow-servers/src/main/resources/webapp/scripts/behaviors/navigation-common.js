Box.Application.addBehavior('navigation-common', function(context) {
  'use strict';
  var windowSvc;
  return {
    init: function() {
      windowSvc = context.getService("window-service");
    },
    destroy: function() {
      windowSvc = null;
    },
    onclick: function(event, element, elementType) {
      switch (elementType) {
        case 'alerts':
          windowSvc.open("alerts.html", "index-alerts");
          break;
      }
    },
    onchange: function(event, element, elementType) {
      switch (elementType) {
        case 'select-timescale':
          context.broadcast("timescale-changed", element.children[element.selectedIndex].getAttribute("value"));
          break;
      }
    },
  };
});