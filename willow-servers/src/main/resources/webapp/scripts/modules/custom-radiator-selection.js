Box.Application.addModule('custom-radiator-selection', function(context) {
  'use strict';

  var graphConfiguration, $, d3, moduleElement, store, windowService;

  function radiatorLabel(data) {
    return "radiator name: " + data;
  }

  function generateRadiatorListing() {
    d3.select(moduleElement).selectAll("div")
        .data(store.customRadiators.listAvailableRadiators())
        .enter()
        .append("li")
        .classed("custom-radiator-selection-item", true)
        .text(radiatorLabel)
        .attr("data-type", "append-to-radiator")
        .attr("data-radiator-id", function (d) { return d; });
  }

  function dispatchConfigurationToRadiator(radiatorId, config) {
    store.customRadiators.appendConfig(radiatorId, config);
    windowService.sendGraphToRadiator(radiatorId, config);
  }

  return {
    init: function() {
      $ = context.getGlobal("jQuery");
      d3 = context.getGlobal("d3");
      moduleElement = context.getElement();

      store = context.getService("configuration-store");
      windowService = context.getService("window");

      graphConfiguration = context.getConfig("graphConfiguration"); //FIXME

      generateRadiatorListing();
    },

    destroy: function() {
      moduleElement = null;
      d3 = null;
      store = null;
    },

    onclick: function(event, element, elementType) {
      var radiatorId;
      switch (elementType) {
        case "create-new-radiator":
          radiatorId = $("input[name=radiator-id]", moduleElement).val();
          dispatchConfigurationToRadiator(radiatorId, graphConfiguration);
          break;
        case "append-to-radiator":
          radiatorId = element.getAttribute("data-radiator-id");
          dispatchConfigurationToRadiator(radiatorId, graphConfiguration);
          break;
      }
    }
  };
});