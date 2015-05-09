Box.Application.addService('cubism-graphs', function(application) {
  'use strict';

  var windowSvc = application.getService('window');
  var cubism    = application.getGlobal('cubism');

  var timescale = windowSvc.getHashVariable('timescale') || 10800;

  var cubismContext = cubism.context();
  var focusEvents   = [];

  cubismContext.on('focus', function(index) {
    focusEvents.forEach(function(eventHandler) {
      eventHandler.call(cubismContext, index);
    });
  }, cubismContext);

  // TODO: this may need debouncing, each graph will call this on it's reset
  function resetCubismContext(step, widthInPixels) {
    cubismContext.step(step)
        .size(widthInPixels);
  }

  // create a context when service initializes
  var widthInPx = $(window).width();
  var step      = parseInt(timescale * 1000 / widthInPx);
  resetCubismContext(step, widthInPx);

  // TODO should we only wrap cubism context handling into this service
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
      // there can only be one focus event per context:
      // event cache will take care of executing all events
      focusEvents.push(eventHandler);
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