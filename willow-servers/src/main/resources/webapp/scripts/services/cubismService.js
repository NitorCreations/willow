Box.Application.addService('cubism-graphs', function(application) {
  'use strict';

  var cubism = application.getGlobal('cubism');

  var cubismContext;

  function resetCubismContext(step, widthInPixels) {
    cubismContext.step(step)
        .size(widthInPixels);
  }

  //TODO should we only wrap cubism context handling into this service
  // or should we set default configurations such as the axis layout?
  return {
    resetCubismContext: resetCubismContext,
    createGraphAxis: function () {
      return cubismContext.axis();
    },
    createRulerOverGraphs: function() {
      return cubismContext.rule();
    },
    onFocus: function(eventHandler) {
      cubismContext.on("focus", eventHandler, cubismContext);
    },
    createMetrics: function(dataSourceRequest, name) {
      return cubismContext.metric(dataSourceRequest, name);
    },
    createHorizonGraph: function() {
      return cubismContext.horizon();
    },
    removeHorizonGraph: function() {
      return cubismContext.horizon().remove;
    }
  };
});