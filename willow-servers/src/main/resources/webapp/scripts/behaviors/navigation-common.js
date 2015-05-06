Box.Application.addBehavior('navigation-common', function(context) {
  'use strict';
  var windowSvc, $, moduleEl, intercom, localStorage, name;
  var timescaleInHash = function(timescale) {
    windowSvc.variableStateInHash("timescale", timescale, function(hash) {
      $("a", moduleEl).each(function(index, element) {
        $(element).attr("href", "#" + windowSvc.addOrReplaceUrlVariable(hash, "metric", element.getAttribute("data-metric")));
      });
    });
  };

  return {
    init: function() {
      windowSvc = context.getService("window");
      $ = context.getGlobal("jQuery");
      moduleEl = context.getElement();
      var timescale = windowSvc.getHashVariable("timescale") || 10800;
      timescaleInHash(timescale);
      intercom = context.getGlobal("Intercom").getInstance();
      localStorage = context.getGlobal("localStorage");
      name = context.getGlobal("name");
      var windowsArr = localStorage.willowWindows ? JSON.parse(localStorage.willowWindows) : [];
      if (windowsArr.indexOf(name) == -1) {
        windowsArr.push(window.name);
        localStorage.willowWindows = JSON.stringify(windowsArr);
      }
      $(window).unload(function() {
        var windowsArr = localStorage.willowWindows ? JSON.parse(localStorage.willowWindows) : [];
        windowsArr = windowsArr.filter(function(value) { return value != name; });
        localStorage.willowWindows = JSON.stringify(windowsArr);
      });
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