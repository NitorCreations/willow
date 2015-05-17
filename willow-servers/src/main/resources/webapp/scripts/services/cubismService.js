Box.Application.addService('cubism-graphs', function(application) {
  'use strict';
  var cubism    = application.getGlobal('cubism');

  var cubismContext = cubism.context();
  var focusEvents   = {};

  cubismContext.on('focus', function(index) {
    for (var moduleId in focusEvents) {
      focusEvents[moduleId].call(cubismContext, index);
    }
  }, cubismContext);

  // TODO: this may need debouncing, each graph will call this on it's reset
  function resetCubismContext(step, widthInPixels) {
    cubismContext.step(step).size(widthInPixels);
    application.broadcast("cubism-context-reset");
  }

  //FIXME service should not be dependent on the DOM on browser state
  function init() {
    var windowSvc = application.getService('window');
    var timescale = windowSvc.getHashVariable('timescale') || 10800;
    var widthInPx = $(window).width();
    var step      = parseInt(timescale * 1000 / widthInPx);
    resetCubismContext(step, widthInPx);
  }

  // create a context when service initializes
  init();

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
    xToTime: function(pageX) {
      var timeStart = cubismContext.scale.domain()[0].getTime();
      return timeStart + (pageX * cubismContext.step());
    }
  };
});