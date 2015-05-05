Box.Application.addBehavior('metric-links', function(context) {
  var utils, $, moduleEl;

  var metricInHash = function(metric) {
    utils.variableStateInHash("metric", metric, function(hash) {
      $("a", moduleEl).each(function(index, element) {
        $(element).attr("class", "");
        $(element).attr("href", "#" + utils.addOrReplaceUrlVariable(hash, "metric", element.getAttribute("id")));
      });
      $("#" + metric, moduleEl).attr("class", "pagename current");
      if ($(window).width() < 500) {
        $("#" + metric, moduleEl).prependTo(moduleEl);
      }
    });
  };

  return {
    init: function() {
      utils = context.getService("utils");
      $ = context.getGlobal("jQuery");
      moduleEl = context.getElement();
      var metric = utils.getHashVariable("metric") || "cpu";
      metricInHash(metric);
    },
    destroy: function() {
      utils = null;
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