// Stop event propagation whenever graph legen is clicked
// this makes sure we don't reset graphs unnecessarily when zooming
Box.Application.addBehavior('legend-click', function(context) {
  var $, moduleEl;

  return {
    init: function() {
      $ = context.getGlobal("jQuery");
      moduleEl = context.getElement();
    },
    onmousedown: function(e, element, elementType) {
      if (e.target && (e.target.nodeName === 'circle' || e.target.nodeName === 'text')) {
        e.stopPropagation();
      }
    }
  };
});
