Box.Application.addBehavior('metric-links', function(context) {
  var windowSvc, $, moduleEl;

  var metricInHash = function(metric) {
    windowSvc.variableStateInHash("metric", metric, function(hash) {
      $("a", moduleEl).each(function(index, element) {
        $(element).attr("class", "");
        $(element).attr("href", "#" + windowSvc.addOrReplaceUrlVariable(hash, "metric", element.getAttribute("id")));
      });
      $("#" + metric, moduleEl).attr("class", "pagename current");
      if ($(window).width() < 500) {
        $("#" + metric, moduleEl).prependTo(moduleEl);
      }
    });
  };

  return {
    init: function() {
      windowSvc = context.getService("window");
      $ = context.getGlobal("jQuery");
      moduleEl = context.getElement();
      var metric = windowSvc.getHashVariable("metric") || "cpu";
      metricInHash(metric);
    },
    destroy: function() {
      windowSvc = null;
    },
    onclick: function(event, element, elementType) {
      if (elementType === 'select-metric') {
        var metric = element.getAttribute("id");
        metricInHash(metric);
        context.broadcast("metric-changed", metric);
      }
    }
  };
});