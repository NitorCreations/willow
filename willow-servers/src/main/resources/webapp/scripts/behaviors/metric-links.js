Box.Application.addBehavior('metric-links', function(context) {
  var utils;
  return {
    init: function() {
      utils = context.getService("utils");
    },
    destroy: function() {
      utils = null;
    },
    onclick: function(event, element, elementType) {
      if (elementType === 'select-metric') {
        context.broadcast("metric-changed", utils.getHashVariable(element.getAttribute("href").substring(1), "metric"));
      }
    }
  };
});