Box.Application.addService('cubism-graphs', function(application) {
  'use strict';
  var cubism    = application.getGlobal('cubism');
  var windowSvc = application.getService('window');
  var utils = application.getService('utils');
  var cubismContext = cubism.context();
  var focusEvents   = {};

  cubismContext.on('focus', function(index) {
    for (var moduleId in focusEvents) {
      focusEvents[moduleId].call(cubismContext, index);
    }
  }, cubismContext);

  // TODO: this may need debouncing, each graph will call this on it's reset
  function resetCubismContext(step, widthInPx, timescale) {
    widthInPx = (typeof widthInPx === 'number') ? widthInPx : $(window).width();
    timescale = (typeof timescale === 'number') ? timescale : windowSvc.getTimescale();
    step = (typeof step === 'number') ? step : utils.getStep(timescale, widthInPx);

    cubismContext.step(step).size(widthInPx);
    application.broadcast("cubism-context-reset");
  }

  // create a context when service initializes
  //FIXME service should not be dependent on the DOM on browser state
  resetCubismContext();

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
    onFocus: function(eventHandler, moduleId) {
      // there can only be one focus event per context:
      // event cache will take care of executing all events
      focusEvents[moduleId] = eventHandler;
    },
    createMetrics: function(dataSourceRequest, name) {
      return cubismContext.metric(dataSourceRequest, name);
    },
    createHorizonGraph: function() {
      return cubismContext.horizon();
    },
    removeHorizonGraph: function() {
      return cubismContext.horizon().remove;
    },
    start: function() {
      return cubismContext.start();
    },
    stop: function() {
      return cubismContext.stop();
    },
    xToTime: function(pageX) {
      var timeStart = cubismContext.scale.domain()[0].getTime();
      return timeStart + (pageX * cubismContext.step());
    }
  };
});