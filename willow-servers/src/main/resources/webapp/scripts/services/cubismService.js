Box.Application.addService('cubism-graphs', function(application) {
  'use strict';

  var cubism = application.getGlobal('cubism');

  var cubismContext;

  function resetCubismContext(step, widthInPixels) {
    cubismContext.step(step)
        .size(widthInPixels);
  }

  // create a context when service initializes
  var widthInPx = $(window).width();
  var step      = parseInt(timescale * 1000 / widthInPx);
  resetCubismContext(step, widthInPx);

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