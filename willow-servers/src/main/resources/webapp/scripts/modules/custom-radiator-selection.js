Box.Application.addModule('custom-radiator-selection', function(context) {
  'use strict';

  var graphConfiguration, $, d3, moduleElement, store, windowService;

  function radiatorLabel(data) {
    return data;
  }

  function renderLatestRadiatorListing() {
    d3.select(moduleElement).select("ul").selectAll("li")
      .remove();
    d3.select(moduleElement).select("ul").selectAll("li")
      .data(store.customRadiators.listAvailableRadiators())
      .enter()
      .append("li")
      .classed("custom-radiator-selection-item btn btn-sm smooth", true)
      .text(radiatorLabel)
      .attr("data-type", "append-to-radiator")
      .attr("data-radiator-id", function (d) { return d; });
  }

  function dispatchConfigurationToRadiator(radiatorId, config) {
    store.customRadiators.appendConfiguration(radiatorId, config);
    windowService.sendGraphToRadiator(radiatorId, config);
  }

  function showDialog() {
    $(moduleElement).dialog({
      modal: true,
      title: "Append graph to radiator..."
    });
  }

  function hideDialog() {
    $(moduleElement).dialog("destroy");
  }

  return {
    init: function() {
      $ = context.getGlobal("jQuery");
      d3 = context.getGlobal("d3");
      moduleElement = context.getElement();

      store = context.getService("configuration-store");
      windowService = context.getService("window");
    },

    destroy: function() {
      moduleElement = null;
      d3 = null;
      store = null;
      graphConfiguration = null;
    },

    onclick: function(event, element, elementType) {
      var radiatorId;
      switch (elementType) {
        case "create-new-radiator":
          var inputField = $("input[name=radiator-id]", moduleElement);
          radiatorId = inputField.val();
          dispatchConfigurationToRadiator(radiatorId, graphConfiguration);
          graphConfiguration = null;
          inputField.val("");
          hideDialog();
          break;
        case "append-to-radiator":
          radiatorId = element.getAttribute("data-radiator-id");
          dispatchConfigurationToRadiator(radiatorId, graphConfiguration);
          graphConfiguration = null;
          hideDialog();
          break;
      }
    },

    messages: ["open-radiator-list"],

    onmessage: function(name, data) {
      switch (name) {
        case "open-radiator-list":
          graphConfiguration = data;
          renderLatestRadiatorListing();
          showDialog();
          break;
      }
    },

    /* only for testing */
    setGraphConfig: function(val) {
      graphConfiguration = val;
    }
  };
});